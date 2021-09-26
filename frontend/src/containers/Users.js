import React, { useState, useEffect } from "react";
import { ListGroup, ListGroupItem, Row, Container } from "react-bootstrap";
import { LinkContainer } from "react-router-bootstrap";
import { useAppContext } from "../libs/contextLib";
import { handleErrors, onError } from "../libs/errorLib";
import "./Users.css";

export default function Users() {
  const [users, setUsers] = useState([]);
  const { userId } = useAppContext();
  const [isLoading, setIsLoading] = useState(true);
  
  useEffect(() => {
    async function onLoad() {
      if (!userId) {
        return;
      }
  
      try {
        let requestedUsers = await fetch(`${window.location.origin}/api/auth/users`)
          .then(handleErrors)
          .then(response => response.json())
        setUsers(requestedUsers)
      } catch (e) {
        onError(e);
      }
  
      setIsLoading(false);
    }
  
    onLoad();
  }, [userId]);

  function renderUsersList(users) {
    return [{}].concat(users).map((user, i) =>
      i !== 0 && (
        <Container>
          <Row>
            <LinkContainer key={user.id} to={`/users/${user.id}`}>
              <ListGroupItem variant="primary" action>
                <Row>
                  <h2><strong>{user.name}</strong></h2>
                </Row>
              </ListGroupItem>
            </LinkContainer>
          </Row>
          <Row>
            <br></br>
          </Row>
        </Container>
      ) 
    );
  }

  function renderUsers() {
    return (
      <div>
        <ListGroup variant="flush">
          {!isLoading && renderUsersList(users)}
        </ListGroup>
      </div>
    );
  }

  return (
    <div>
      {userId && renderUsers()}
    </div>
  );
}