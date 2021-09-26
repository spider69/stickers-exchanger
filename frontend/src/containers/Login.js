import React, { useState } from "react";
import { useAppContext } from "../libs/contextLib";
import { FormGroup, FormControl, FormLabel } from "react-bootstrap";
import LoaderButton from "../components/LoaderButton";
import { useHistory } from "react-router-dom";
import { useFormFields } from "../libs/hooksLib";
import { onError } from "../libs/errorLib";
import "./Login.css";


export default function Login() {
    const history = useHistory();
    const { setUserId } = useAppContext();
    const [isLoading, setIsLoading] = useState(false);
    const [fields, handleFieldChange] = useFormFields({
        login: "",
        password: ""
    });

    function validateForm() {
        return fields.login.length > 0 && fields.password.length > 0;
    }

    async function handleLoginResponse(response) {
        let responseText = await response.text()
        if (!response.ok) {
            throw Error(responseText);
        }
        return responseText;
    }

    async function handleSubmit(event) {
        event.preventDefault();

        setIsLoading(true);

        try {
            let data = { "login": fields.login, "password": fields.password }
            let userId = await fetch(`${window.location.origin}/api/auth/sign_in`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            }).then(handleLoginResponse)
            setUserId(userId)
            history.push("/");
        } catch (e) {
            onError(e);
            setIsLoading(false);
        }
    }

    return (
        <div className="Login">
            <form onSubmit={handleSubmit}>
                <FormGroup controlId="login">
                    <FormLabel>Login</FormLabel>
                    <FormControl
                        autoFocus
                        type="text"
                        value={fields.login}
                        onChange={handleFieldChange}
                    />
                </FormGroup>
                <FormGroup controlId="password">
                    <FormLabel>Password</FormLabel>
                    <FormControl
                        type="password"
                        value={fields.password}
                        onChange={handleFieldChange}
                    />
                </FormGroup>
                <LoaderButton
                    block
                    type="submit"
                    isLoading={isLoading}
                    disabled={!validateForm()}
                >
                    Login
               </LoaderButton>
            </form>
        </div>
    );
}