import React from "react";
import { Route, Redirect } from "react-router-dom";
import { useAppContext } from "../libs/contextLib";

export default function UnauthenticatedRoute({ children, ...rest }) {
  const { userId } = useAppContext();
  return (
    <Route {...rest}>
      {!userId ? (
        children
      ) : (
        <Redirect to="/" />
      )}
    </Route>
  );
}