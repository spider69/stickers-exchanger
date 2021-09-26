import React, { useState, useEffect } from "react";
import { ListGroup, ListGroupItem, Row, Col, Container } from "react-bootstrap"
import { LinkContainer } from "react-router-bootstrap";
import { useParams } from "react-router-dom";
import { useAppContext } from "../libs/contextLib"
import { handleErrors, onError } from "../libs/errorLib";
import "./UserPage.css"

export default function UserPage() {
    const { id } = useParams();
    const { userId } = useAppContext();
    const [user, setUser] = useState(null);
    const [collections, setCollections] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        async function onLoad() {
            if (!userId) {
                return;
            }

            try {
                let requestedUser = await fetch(`${window.location.origin}/api/auth/user/${id}`)
                    .then(handleErrors)
                    .then(response => response.json())
                setUser(requestedUser);

                let userCollections = await fetch(`${window.location.origin}/api/user/collections?userId=${id}`)
                    .then(handleErrors)
                    .then(response => response.json())
                setCollections(userCollections)
            } catch (e) {
                onError(e);
            }

            setIsLoading(false);
        }

        onLoad();
    }, [id, userId]);

    function renderCollectionsList(collections) {
        return [{}].concat(collections).map((collection, i) =>
          i !== 0 && (
            <LinkContainer key={collection.id} to={`/collections/${collection.id}?userId=${id}`}>
              <ListGroupItem variant="primary" action>
                 {collection.name}
              </ListGroupItem>
            </LinkContainer>
          ) 
        );
    }    

    return (
        <Container className="form">
            {user && (
                <Container className="form">
                    <Row>
                        <Col>
                            <h3>
                                <strong>Collector's name:</strong>
                            </h3>
                            <h2>
                                {user.name}
                            </h2>
                            <h3>
                                <strong>Email:</strong> 
                            </h3>
                            <h2>
                                {user.email}
                            </h2>
                        </Col>
                        <Col>
                            <h3>
                                <strong>Collections</strong>
                            </h3>
                            <h2>
                                <ListGroup variant="flush">
                                    {!isLoading && renderCollectionsList(collections)}
                                </ListGroup>
                            </h2>
                        </Col>
                    </Row>
                </Container>
            )}
        </Container>
    );
}