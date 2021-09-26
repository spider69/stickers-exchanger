import React, { useState, useEffect } from "react";
import { AppContext } from "./libs/contextLib";
import { Nav, Navbar } from "react-bootstrap";
import { useHistory } from "react-router-dom";
import { handleErrors } from "./libs/errorLib";
import Routes from "./Routes";
import "./App.css"

function App() {
  const history = useHistory();
  const [isAuthenticating, setIsAuthenticating] = useState(true);
  const [userId, setUserId] = useState(null);
  
  async function handleCurrentSessionResponse(response) {
    let responseText = await response.text()
    if (!response.ok) {
      throw Error(responseText);
    }
    return responseText;
  }

  useEffect(() => {
    async function onLoad() {
      try {
        let id = await fetch(`${window.location.origin}/api/auth/check_session`)
          .then(handleCurrentSessionResponse)
        setUserId(id);
      }
      catch (e) {
        console.log(e)
      }
    
      setIsAuthenticating(false);
    }

    onLoad();
  }, []);

  function handleUsers() {
    history.push("/users")
  }

  function handleCollections() {
    history.push(`/collections?userId=${userId.replace(/"/g, "")}`)
  }

  async function handleLogout() {
    await fetch(`${window.location.origin}/api/auth/sign_out`, { method: 'DELETE' }).then(handleErrors);
    setUserId(null);
    history.push("/login");
  }

  return (
    !isAuthenticating &&
    <div className="App container">
      <Navbar expand="lg" bg="dark" variant="dark">
        <Navbar.Brand href="/"><h1>Stickers exchanger</h1></Navbar.Brand>
        <Nav className="mr-auto">
          {userId
            ? <>
              <Nav.Link onClick={handleUsers}><h3>Users</h3></Nav.Link>
              <Nav.Link onClick={handleCollections}><h3>Collections</h3></Nav.Link>
              <Nav.Link onClick={handleLogout}><h3>Logout</h3></Nav.Link>
            </>
            : <>
              <Nav.Link href="login"><h3>Login</h3></Nav.Link>
              <Nav.Link href="signup"><h3>Sign up</h3></Nav.Link>
            </>
          }
        </Nav>
      </Navbar>
      <AppContext.Provider value={{ userId, setUserId }}>
        <Routes />
      </AppContext.Provider>
    </div>
  );
}

export default App;