import React, { useState, useEffect } from "react";
import { ListGroup, ListGroupItem, Button, Container, Row, Col } from "react-bootstrap";
import { FormGroup, FormControl, FormLabel } from "react-bootstrap";
import Image from 'react-bootstrap/Image';
import { LinkContainer } from "react-router-bootstrap";
import { useParams, useLocation } from "react-router-dom";
import { useAppContext } from "../libs/contextLib";
import { handleErrors, onError } from "../libs/errorLib";
import "./CollectionPage.css"

function useQuery() {
    return new URLSearchParams(useLocation().search);
}

function useFormFields(initialState) {
    const [fields, setValues] = useState(initialState);
  
    return [
      fields,
      function(newFields) {
        setValues(newFields);
      },
      function(event, stickerId) {
        setValues({...fields, [stickerId]: {"count": +event.target.value, "changed": true}});
      }
    ];
}

export default function CollectionPage() {
    const { id } = useParams();
    const query = useQuery();
    const { userId } = useAppContext();
    const [collection, setCollection] = useState(null);
    const [stickers, setStickers] = useState([])
    const [isLoading, setIsLoading] = useState(true);
    const [stickersCounts, setStickersCounts, handleCountChange] = useFormFields({});
    const [user, setUser] = useState(null);
    var currentUserId = query.get("userId") 
    if (currentUserId === undefined) {
        currentUserId = userId.replace(/"/g, "")
    }

    useEffect(() => {
        async function onLoad() {
            try {
                if (!userId) {
                    return
                }

                let requestedUser = await fetch(`${window.location.origin}/api/auth/user/${currentUserId}`)
                    .then(handleErrors)
                    .then(response => response.json())
                setUser(requestedUser);

                let requestedCollection = await fetch(`${window.location.origin}/api/collections/${id}`)
                    .then(handleErrors)
                    .then(response => response.json())
                setCollection(requestedCollection);

                let stickersRelations = await fetch(`${window.location.origin}/api/user/stickers/relations?collectionId=${id}&userId=${currentUserId}`)
                    .then(handleErrors)
                    .then(response => response.json())
                setStickers(stickersRelations)

                var stickersCountsDict = {}
                stickersRelations.forEach(stickerRelation => 
                    stickersCountsDict[stickerRelation.sticker.id] = {"count": stickerRelation.count, "changed": false}
                )
                setStickersCounts(stickersCountsDict)
            } catch (e) {
                onError(e);
            }

            setIsLoading(false)
        }

        onLoad();
    }, [id]);

    async function onAddSticker(stickerId) {
        try {
            await fetch(`${window.location.origin}/api/user/stickers/${stickerId}?count=0`, {method: 'PUT'})
                .then(handleErrors)

            let collectionStickers = await fetch(`${window.location.origin}/api/user/stickers/relations?collectionId=${id}&userId=${currentUserId}`)
                .then(handleErrors)
                .then(response => response.json())
            setStickers(collectionStickers)
        } catch (e) {
            onError(e);
        }
    }
    
    async function onRemoveSticker(stickerId) {
        try {
            await fetch(`${window.location.origin}/api/user/stickers/${stickerId}`, {method: 'DELETE'})
                .then(handleErrors)

            let collectionStickers = await fetch(`${window.location.origin}/api/user/stickers/relations?collectionId=${id}&userId=${currentUserId}`)
                .then(handleErrors)
                .then(response => response.json())
            setStickers(collectionStickers)
            setStickersCounts({...stickersCounts, [stickerId]: {"count": 0, "changed": false}})
        } catch (e) {
            onError(e);
        }
    }

    async function handleUpdate(stickerId) {
        try {
            if (stickersCounts[stickerId].count >= 0) {
                await fetch(`${window.location.origin}/api/user/stickers/update/${stickerId}?count=${stickersCounts[stickerId].count}`, { method: 'PUT' })
                    .then(handleErrors)
                setStickersCounts({...stickersCounts, [stickerId]: {"count": stickersCounts[stickerId].count, "changed": false}})
            } else {
                onError("Count for exchange must be >= 0")
            }
        } catch (e) {
            onError(e)
        }
    }

    function renderStickersList(stickersRelations) {
        let loggedUserId = userId.replace(/"/g, "")
        let viewLoggedUser = currentUserId === loggedUserId

        return [{}].concat(stickersRelations).map((stickerRelation, i) =>
          i !== 0 && (
        <Container>
          <Row>
            <Col sm={6}>
              <LinkContainer key={stickerRelation.sticker.id} to={`../stickers/${stickerRelation.sticker.id}`}>
                <ListGroupItem variant={stickerRelation.belongsToUser ? "success" : "danger"} action>
                  <Row>
                    <Col sm={4}>
                      <Image src={`../stickers/${stickerRelation.sticker.image}`} thumbnail />
                    </Col>
                    <Col sm={8}>
                      <h2><strong>{stickerRelation.sticker.number}</strong></h2>
                      {stickerRelation.sticker.description}
                    </Col>
                  </Row>
                </ListGroupItem>
              </LinkContainer>
            </Col>
            <Col sm={2}>
                <Button 
                    variant={stickerRelation.belongsToUser ? "danger" : "success"}
                    onClick={() => stickerRelation.belongsToUser ? onRemoveSticker(stickerRelation.sticker.id) : onAddSticker(stickerRelation.sticker.id)} 
                    size="lg"
                    disabled={!viewLoggedUser}
                >
                    {stickerRelation.belongsToUser ? 'Remove' : 'Add'}
                </Button>
            </Col>
            <Col sm="auto">
                <FormGroup controlId="count">
                    <FormLabel>Count for exchange</FormLabel>
                    <FormControl 
                        type="number" 
                        value={stickersCounts[stickerRelation.sticker.id].count} 
                        onChange={(event) => handleCountChange(event, stickerRelation.sticker.id)}
                        disabled={!viewLoggedUser || stickerRelation.belongsToUser === false} 
                    />
                </FormGroup>
                <Button
                    block
                    disabled={!viewLoggedUser || stickerRelation.belongsToUser === false || !stickersCounts[stickerRelation.sticker.id].changed}
                    onClick={() => handleUpdate(stickerRelation.sticker.id)}
                >
                    Update
                </Button>
            </Col>
          </Row>
          <Row>
              <br></br>
          </Row>             
        </Container>
          ) 
        );
    }

    return (
        <Container className="form">
            {userId && collection && 
            <Container className="form">
                <Row className="justify-content-md-center">
                    <h3>
                        {collection.name}
                    </h3>
                </Row>
                <Row>
                    <Col>
                        <Image src={collection.image} thumbnail />
                    </Col>
                    <Col>
                        <h2>
                            <strong>Collector:</strong>
                        </h2>
                        <LinkContainer key={user.id} to={`../../users/${user.id}`}>
                            <Button variant="secondary" size="lg">
                                {user.name}
                            </Button>
                        </LinkContainer>
                        <h2>
                            <strong>Number of stickers:</strong>
                        </h2>
                        <h2>
                             {collection.numberOfStickers}
                        </h2>
                        <h2>
                            <strong>Description:</strong> 
                        </h2>
                        <h2>
                            {collection.description}
                        </h2>
                    </Col>
                </Row>
                <Row>
                    <ListGroup variant="flush">
                        {!isLoading && renderStickersList(stickers)}
                    </ListGroup>
                </Row>
            </Container>
            }
        </Container>
    );
}