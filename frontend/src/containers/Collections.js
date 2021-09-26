import React, { useState, useEffect } from "react";
import { ListGroup, ListGroupItem, Button, Row, Col, Container } from "react-bootstrap";
import { useLocation } from "react-router-dom";
import { LinkContainer } from "react-router-bootstrap";
import { useAppContext } from "../libs/contextLib";
import { handleErrors, onError } from "../libs/errorLib";
import Image from 'react-bootstrap/Image'
import "./Collections.css";

function useQuery() {
  return new URLSearchParams(useLocation().search);
}

export default function Collections() {
  const query = useQuery();
  const [collectionsRelations, setCollectionsRelations] = useState([]);
  const { userId } = useAppContext();
  const [isLoading, setIsLoading] = useState(true);
  var currentUserId = query.get("userId") 
  if (currentUserId === undefined) {
      currentUserId = userId.replace(/"/g, "")
  }
  
  useEffect(() => {
    async function onLoad() {
      if (!userId) {
        return;
      }

      try {
        let requestedCollectionsRelations = await fetch(`${window.location.origin}/api/user/collections/relations?userId=${currentUserId}`)
          .then(handleErrors)
          .then(response => response.json())
        setCollectionsRelations(requestedCollectionsRelations)
      } catch (e) {
        onError(e);
      }
  
      setIsLoading(false);
    }
  
    onLoad();
  }, [userId, currentUserId]);

  async function onAddCollection(collectionId) {
    try {
        await fetch(`${window.location.origin}/api/user/collections/${collectionId}`, {method: 'PUT'})
            .then(handleErrors)

        let requestedCollectionsRelations = await fetch(`${window.location.origin}/api/user/collections/relations?userId=${currentUserId}`)
          .then(handleErrors)
          .then(response => response.json())
        setCollectionsRelations(requestedCollectionsRelations)
    } catch (e) {
        onError(e);
    }
  }

  async function onRemoveCollection(collectionId) {
    try {
        await fetch(`${window.location.origin}/api/user/collections/${collectionId}`, {method: 'DELETE'})
            .then(handleErrors)

        let requestedCollectionsRelations = await fetch(`${window.location.origin}/api/user/collections/relations?userId=${currentUserId}`)
            .then(handleErrors)
            .then(response => response.json())
        setCollectionsRelations(requestedCollectionsRelations)
    } catch (e) {
        onError(e);
    }
  }

  function renderCollectionsList(collectionsRelations) {
    let loggedUserId = userId.replace(/"/g, "")
    let viewLoggedUser = currentUserId === loggedUserId

    return [{}].concat(collectionsRelations).map((collectionRelation, i) =>
      i !== 0 && (
        <Container>
          <Row>
            <Col sm={8}>
              <LinkContainer key={collectionRelation.collection.id} to={`/collections/${collectionRelation.collection.id}?userId=${currentUserId}`}>
                <ListGroupItem variant={collectionRelation.belongsToUser ? "success" : "danger"} action>
                  <Row>
                    <Col sm={4}>
                      <Image src={`collections/${collectionRelation.collection.image}`} thumbnail />
                    </Col>
                    <Col sm={8}>
                      <h2><strong>{collectionRelation.collection.name}</strong></h2>
                      <h3>{collectionRelation.collection.description}</h3>
                    </Col>
                  </Row>
                </ListGroupItem>
              </LinkContainer>
            </Col>
            <Col sm={4}>
                <Button 
                    variant={collectionRelation.belongsToUser ? "danger" : "success"} 
                    onClick={() => collectionRelation.belongsToUser ? onRemoveCollection(collectionRelation.collection.id) : onAddCollection(collectionRelation.collection.id)} 
                    size="lg"
                    disabled={!viewLoggedUser}
                >
                    {collectionRelation.belongsToUser ? 'Remove' : 'Add'}
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

  function renderCollections() {
    return (
      <Container>
        <ListGroup variant="flush">
          {!isLoading && renderCollectionsList(collectionsRelations)}
        </ListGroup>
      </Container>
    );
  }

  return (
    <Container>
      {userId && renderCollections()}
    </Container>
  );
}