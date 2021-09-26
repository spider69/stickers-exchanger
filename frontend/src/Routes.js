import React from "react";
import { Route, Switch } from "react-router-dom";
import Home from "./containers/Home";
import NotFound from "./containers/NotFound";
import Login from "./containers/Login";
import Signup from "./containers/Signup";
import AuthenticatedRoute from "./components/AuthenticatedRoute";
import UnauthenticatedRoute from "./components/UnauthenticatedRoute";
import Collections from "./containers/Collections"
import CollectionPage from "./containers/CollectionPage";
import StickerPage from "./containers/StickerPage";
import UserPage from "./containers/UserPage";
import Users from "./containers/Users";

export default function Routes() {
    return (
        <Switch>
            <Route exact path="/">
                <Home />
            </Route>
            <AuthenticatedRoute exact path="/collections">
                <Collections />
            </AuthenticatedRoute>
            <AuthenticatedRoute exact path="/collections/:id">
                <CollectionPage />
            </AuthenticatedRoute>
            <AuthenticatedRoute exact path="/stickers/:id">
                <StickerPage/>
            </AuthenticatedRoute>
            <AuthenticatedRoute exact path="/home">
                <Home />
            </AuthenticatedRoute>
            <AuthenticatedRoute exact path="/users">
                <Users />
            </AuthenticatedRoute>
            <AuthenticatedRoute exact path="/users/:id">
                <UserPage />
            </AuthenticatedRoute>
            <UnauthenticatedRoute exact path="/login">
                <Login />
            </UnauthenticatedRoute>
            <UnauthenticatedRoute exact path="/signup">
                <Signup />
            </UnauthenticatedRoute>
            <Route>
                <NotFound />
            </Route>

        </Switch>
    );
}