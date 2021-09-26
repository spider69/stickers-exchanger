import React from "react";
import { useHistory } from "react-router-dom";
import { useAppContext } from "../libs/contextLib";
import "./Home.css";

export default function Home() {
  const history = useHistory();
  const { userId } = useAppContext();

  function renderLander() {
    return (
      <div className="lander">
        <h1>Welcome to stickers exchanger service!</h1>
        <p>Click sign up to register</p>
      </div>
    );
  }

  function redirectToUserPage() {
    history.push(`/users/${userId.replace(/"/g, "")}`);
  }

  return (
    <div className="Home">
      {userId ? redirectToUserPage() : renderLander()}
    </div>
  );
}