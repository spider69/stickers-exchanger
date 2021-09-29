import React, { useState, useEffect } from "react";
import { ListGroup, ListGroupItem, Row, Container, Col } from "react-bootstrap";
import { LinkContainer } from "react-router-bootstrap";
import { useAppContext } from "../libs/contextLib";
import { handleErrors, onError } from "../libs/errorLib";
import "./ExchangesPage.css";

export default function ExchangesPage() {
  const [exchanges, setExchanges] = useState([]);
  const { userId } = useAppContext();
  const [isLoading, setIsLoading] = useState(true);
  
  useEffect(() => {
    async function onLoad() {
      if (!userId) {
        return;
      }
  
      try {
        let requestedExchanges = await fetch(`${window.location.origin}/api/exchanges`)
          .then(handleErrors)
          .then(response => response.json())
        setExchanges(requestedExchanges)
      } catch (e) {
        onError(e);
      }
  
      setIsLoading(false);
    }
  
    onLoad();
  }, [userId]);

  function renderExchangesList(exchanges) {
    return [{}].concat(exchanges).map((exchange, i) =>
      i !== 0 && (
            <Container>
                <Row>
                    <Col>
                        <LinkContainer key={exchange.id} to={`/users/${exchange.userId}`}>
                            <ListGroupItem variant="primary" action>
                                <h2>to user</h2>
                            </ListGroupItem>
                        </LinkContainer>
                    </Col>
                    <Col>
                        <Container className="form">
                            <h2>
                                <strong>User id: {exchange.userId}</strong>
                            </h2>
                            <h2>
                                <strong>Stickers needed to user: {exchange.stickersNeededToUser.map(s => { return <h2>{s}</h2> })}</strong>
                            </h2>
                            <h2>
                                <strong>Stickers needed from user: {exchange.stickersNeededFromUser.map(s => { return <h2>{s}</h2> })}</strong>
                            </h2>
                        </Container>
                    </Col>

                </Row>
                <Row>
                    <br></br>
                </Row>
            </Container>
      ) 
    );
  }

  function renderExchanges() {
    return (
      <div>
        <ListGroup variant="flush">
          {!isLoading && renderExchangesList(exchanges)}
        </ListGroup>
      </div>
    );
  }

  return (
    <div>
      {userId && renderExchanges()}
    </div>
  );
}