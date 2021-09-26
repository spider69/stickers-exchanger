import React, { useState, useEffect } from "react";
import { Container, Row, Col, Button } from "react-bootstrap";
import { LinkContainer } from "react-router-bootstrap"
import Image from 'react-bootstrap/Image'
import { useParams } from "react-router-dom";
import { useAppContext } from "../libs/contextLib";
import { handleErrors, onError } from "../libs/errorLib";
import "./StickerPage.css"

export default function StickerPage() {
    const { userId } = useAppContext();
    const { id } = useParams();
    const [sticker, setSticker] = useState(null);
    const [users, setUsers] = useState([])

    useEffect(() => {
        async function onLoad() {
            if (!userId) {
                return
            }

            try {
                let requestedSticker = await fetch(`${window.location.origin}/api/stickers/${id}`)
                    .then(handleErrors)
                    .then(response => response.json())
                setSticker(requestedSticker);

                let usersHavingSticker = await fetch(`${window.location.origin}/api/user/stickers/users_by/${id}`)
                    .then(handleErrors)
                    .then(response => response.json())
                setUsers(usersHavingSticker)
            } catch (e) {
                onError(e);
            }
        }

        onLoad();
    }, [id, userId]);

    function renderUsersList(users) {
        return [{}].concat(users).map((user, i) =>
        i !== 0 && (
            <Col sm="auto">
                <LinkContainer key={user.id} to={`../../users/${user.id}`}>
                    <Button variant="secondary" size="lg">
                        {user.name}
                    </Button>
                 </LinkContainer>
            </Col>
        ))
    }

    return (
        <Container className="form">
            {userId && sticker && (
                <Container>
                    <Row className="justify-content-md-center">
                        <h3>
                            {sticker.number}
                        </h3>
                    </Row>
                    <Row className="info">
                        <Col className="left">
                            <Image src={sticker.image} thumbnail />
                        </Col>
                        <Col className="right">
                            <h2>
                                <strong>Description</strong> {sticker.description}
                            </h2>
                        </Col>
                    </Row>
                    <Row>
                        <h2>
                            <strong>Collectors having this sticker:</strong>
                        </h2>
                    </Row>
                    <Row>
                        {renderUsersList(users)}
                    </Row>
                </Container>
            )}
        </Container>
    );
}