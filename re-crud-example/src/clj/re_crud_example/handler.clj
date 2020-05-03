(ns re-crud-example.handler
  (:require [re-crud-example.db :as db]
            [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]))

(s/defschema UserParams
  {:first-name s/Str
   :last-name s/Str
   :email s/Str})

(s/defschema User
  (assoc UserParams
         :id s/Int))

(s/defschema TodoParams
  {:title s/Str
   :notes s/Str
   :tags s/Str})

(s/defschema Todo
  (merge TodoParams
         {:id s/Int
          :user-id s/Int}))

(def handler
  (api
   {:swagger
    {:ui "/swagger/"
     :spec "/swagger.json"
     :data {:info {:title "re-crud-example"
                   :description "API for re-crud example"}
            :tags [{:name "api", :description "some apis"}]}}}

   (GET "/users.json" []
        :swagger {:operationId "listUsers"}
        :return [User]
        :summary "List all Users"
        (ok (db/get-users)))

   (POST "/users.json" []
         :swagger {:operationId "createUser"}
         :return User
         :body [user UserParams]
         :summary "Create a User"
         (ok (db/add-user user)))

   (context "/users" []

            (GET "/:user-id.json" [user-id]
                 :swagger {:operationId "getUser"}
                 :return User
                 :path-params [user-id :- s/Int]
                 :summary "Get a User"
                 (if-let [user (db/get-user user-id)]
                   (ok user)
                   (not-found)))

            (PATCH "/:user-id.json" [user-id]
                   :swagger {:operationId "updateUser"}
                   :return User
                   :body [user UserParams]
                   :path-params [user-id :- s/Int]
                   :summary "Update a User"
                   (if-let [user (db/update-user user-id user)]
                     (ok user)
                     (not-found)))

            (DELETE "/:user-id.json" [user-id]
                    :swagger {:operationId "deleteUser"}
                    :path-params [user-id :- s/Int]
                    :summary "Delete a User"
                    (if-let [_ (db/delete-user user-id)]
                      (ok)
                      (not-found))))

   (context "/users/:user-id" [user-id]

            :path-params [user-id :- s/Int]

            (GET "/todos.json" []
                 :swagger {:operationId "listTodos"}
                 :return [Todo]
                 :summary "List all Todos for this User"
                 (if-let [user (db/get-user user-id)]
                   (ok (db/get-todos user-id))
                   (not-found "User doesn't exist!")))

            (POST "/todos.json" []
                  :swagger {:operationId "createTodo"}
                  :return Todo
                  :body [todo TodoParams]
                  :summary "Create a Todo for a User"
                  (if-let [user (db/get-user user-id)]
                    (ok (db/add-todo user-id todo))
                    (not-found "User doesn't exist!")))

            (context "/todos" []

                     (GET "/:todo-id.json" [todo-id]
                          :swagger {:operationId "getTodo"}
                          :return Todo
                          :path-params [todo-id :- s/Int]
                          :summary "Get a Todo"
                          (if-let [todo (db/get-todo user-id todo-id)]
                            (ok todo)
                            (not-found)))

                     (PATCH "/:todo-id.json" [todo-id]
                            :swagger {:operationId "updateTodo"}
                            :return Todo
                            :body [todo TodoParams]
                            :path-params [todo-id :- s/Int]
                            :summary "Update a Todo"
                            (if-let [todo (db/update-todo user-id todo-id todo)]
                              (ok todo)
                              (not-found)))

                     (DELETE "/:todo-id.json" [todo-id]
                             :swagger {:operationId "deleteTodo"}
                             :path-params [todo-id :- s/Int]
                             :summary "Delete a Todo"
                             (if-let [_ (db/delete-todo user-id todo-id)]
                               (ok)
                               (not-found)))))))
