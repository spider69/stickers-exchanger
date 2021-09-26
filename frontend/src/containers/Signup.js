import React, { useState } from "react";
import { FormGroup, FormControl, FormLabel } from "react-bootstrap";
import LoaderButton from "../components/LoaderButton";
import { useFormFields } from "../libs/hooksLib";
import { onError } from "../libs/errorLib";
import "./Signup.css";

export default function Signup() {
    const [fields, handleFieldChange] = useFormFields({
        userName: "",
        email: "",
        password: "",
        confirmPassword: ""
    });

    const [isLoading, setIsLoading] = useState(false);

    function validateForm() {
        return (
            fields.userName.length > 0 &&
            fields.email.length > 0 &&
            fields.password.length > 0 &&
            fields.password === fields.confirmPassword
        );
    }

    async function handleSignUpResponse(response) {
        let errorMessage = await response.text()
        if (!response.ok) {
            throw Error(errorMessage);
        }
        return response;
      }

    async function handleSubmit(event) {
        event.preventDefault();

        setIsLoading(true);

        try {
            let data = { "login": fields.userName, "email": fields.email, "password": fields.password }
            await fetch(`${window.location.origin}/api/auth/sign_up`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            }).then(handleSignUpResponse)
            setIsLoading(false);
            window.location.reload(false);
        } catch (e) {
            onError(e)
            setIsLoading(false);
        }
    }

    return (
        <div className="Signup">
            <form onSubmit={handleSubmit}>
                <FormGroup controlId="userName">
                    <FormLabel>User name</FormLabel>
                    <FormControl
                        autoFocus
                        type="text"
                        value={fields.userName}
                        onChange={handleFieldChange}
                    />
                </FormGroup>
                <FormGroup controlId="email">
                    <FormLabel>Email</FormLabel>
                    <FormControl
                        type="email"
                        value={fields.email}
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
                <FormGroup controlId="confirmPassword">
                    <FormLabel>Confirm Password</FormLabel>
                    <FormControl
                        type="password"
                        onChange={handleFieldChange}
                        value={fields.confirmPassword}
                    />
                </FormGroup>
                <LoaderButton
                    block
                    type="submit"
                    isLoading={isLoading}
                    disabled={!validateForm()}
                >
                    Sign up
               </LoaderButton>
            </form>
        </div>
    );
}