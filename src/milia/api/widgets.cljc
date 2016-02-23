(ns milia.api.widgets
  (:refer-clojure :exclude [list])
  (:require [milia.api.http :refer [parse-http]]
            [milia.utils.remote :refer [make-url]]))

(defn generate-content-object-url
  "The Ona API expects clients to submit a URL as the value of the object bound
   to a widget. This is basically asking clients to be aware of internal API
   implementation details since this is a Django REST framework peculiarity.
   This function provides an abstraction that allows Clojure based clients to
   be ignorant of the quirk.
   `content-type` can be either :form or :dataview
   `content-id` is an integer identifying the object"
  [content-type content-id]
  (make-url (case content-type
             :form "forms"
             :dataview "dataviews")
            content-id))

(defn create
  "Create a new widget.
   `widget-definition` is a map containing the following keys:
    `:title` is a string
    `:content_type` is one of either :form of :dataview
    `:content_id` is an intger identifying the content object
    `:description` is a string.
    `:widget_type` is a string, determined by the client e.g. chart
    `:view_type` is a string, determined by the client e.g. bar-chart
    `:column` is the  data column to be stored based on the form field.
    `:group_by` the data column for the data to be grouped by. Optional"
  [{:keys [content_type
           content_id]
    :as widget-definition}]
  (let [url (make-url "widgets")
        processed-widget-definition
        (assoc widget-definition
          :content_object
          (generate-content-object-url
           content_type
           content_id))]
    (parse-http :post
                url
                :http-options {:form-params processed-widget-definition
                               :content-type :json})))

(defn list
  "List widgets belonging to a particular user
   Can optionally be filtered by supplying either a dataview ID or an XForm ID
   Note that the filters are mutually exclusive"
  [& {:keys [dataview-id xform-id]}]
  (parse-http :get
              (make-url (cond
                          dataview-id (str "widgets?dataviewid="
                                           dataview-id)
                          xform-id (str "widgets?xform="
                                        xform-id)
                          :else "widgets"))
              :http-options {:content-type :json}))
