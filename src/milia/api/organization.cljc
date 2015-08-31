(ns milia.api.organization
  (:require [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]))

(def internal-members-team-name "members")

(def owners-team-name "Owners")

(def editor-role "editor")

(defn all
  "List all the organizations belonging to the account making the request.
   When a username is provided, return only those organizations shared by both
   the account making the request and the user associated with the username."
  ([]
   (all nil))
  ([username]
   (let [url (make-url (if username
                         (str "orgs?shared_with=" username)
                         "orgs"))]
     (parse-http :get url))))

(defn create [data]
  (let [url (make-url "orgs")]
    (parse-http :post url :http-options {:form-params data}
                :suppress-4xx-exceptions? true
                :as-map? true)))
(defn profile
  [org-name & {:keys [no-cache?]}]
  (let [url (make-url "orgs" org-name)]
    (parse-http :get url :no-cache? no-cache?)))

(defn can-user-create-project-under-organization?
  "Return whether a user can create projects within an organization
   organization-list is an optional argument"
  [username-to-check organization]
  (let [role
        (->> organization
             :users
             (filter #(= (:user %) username-to-check))
             first
             :role)]
    (or (= role "manager")
        (= role "owner"))))

(defn teams-all
  "Return all the teams for an organization."
  []
  (let [url (make-url "teams")]
    (parse-http :get url)))

(defn teams
  "Return the teams for an organization, removing 'members' team that is used
   internall by the API to store non-team based org members."
  [org-name]
  (let [teams (teams-all)]
    (remove #(or (= internal-members-team-name (:name %))
                 (not= org-name (:organization %))) teams)))

(defn team-info [org-name team-id]
  (let [url (make-url "teams" org-name team-id)]
    (parse-http :get url)))

(defn team-members [team-id]
  (let [url (make-url "teams" team-id "members")]
    (parse-http :get url)))

(defn create-team
  "Add a team to an organization"
  [params]
  (let [url (make-url "teams")]
    (parse-http :post url :http-options {:form-params params})))

(defn add-team-member
  "Add a user to a team"
  [org-name team-id user]
  (let [url (make-url "teams" org-name team-id "members")]
    (parse-http :post url :http-options {:form-params user})))

(defn members [org-name]
  (let [url (make-url "orgs" org-name "members")]
    (parse-http :get url)))

(defn add-member
  "Add a user to an organization"
  ([org-name member]
    (add-member org-name member nil))
  ([org-name member role]
    (let [url (make-url "orgs" org-name "members")
          assigned-role (if role
                          role
                          editor-role)]
      (parse-http :post
                  url
                  :http-options {:form-params {:username member :role assigned-role}}
                  :suppress-4xx-exceptions? true
                  :as-map? true))))

(defn remove-member
  "Remove a user from an organization or organization team"
  ([org-name member]
     (remove-member org-name member nil))
  ([org-name member team-id]
     (let [url (if team-id
                 (make-url "teams" org-name team-id "members")
                 (make-url "orgs" org-name "members"))]
       (parse-http :delete url :http-options {:query-params {:username member}}))))

(defn single-owner?
  "Is the user the only member of the Owners team."
  ([team members]
   (and (= owners-team-name (:name team))
        (= 1 (count members)))))

(defn single-owner-member?
  "Is user only members in org with owner role?"
  [org-name]
  (let [org (profile org-name)
        users (:users org)
        owner-roles (filter #(= "owner" %) (map :role users))]
    (= (count owner-roles) 1)))

(defn update
  "update organization profile"
  [params]
  (let [url (make-url "orgs" (:org params))
        params (dissoc params :org)]
    (parse-http :patch
                url
                :http-options {:form-params params :content-type :json}
                :raw-response? true
                :as-map? true)))

(defn get-team
  "Returns an Organizaion team given the team name."
  [org-name team-name]
  (let [url (make-url (str "teams?org=" org-name))
        teams (parse-http :get url :suppress-4xx-exceptions? true)]
    (first (remove #(not= team-name (:name %)) teams))))

(defn share-team [team-id data]
  "Changes default_role permissions on a project for a team"
  (let [url (make-url "teams" team-id "share")]
    (parse-http :post url :http-options {:form-params data})))
