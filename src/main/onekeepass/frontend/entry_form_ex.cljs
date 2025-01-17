(ns onekeepass.frontend.entry-form-ex
  (:require [clojure.string :as str]
            [onekeepass.frontend.common-components :as cc :refer [alert-dialog-factory
                                                                  enter-key-pressed-factory
                                                                  list-items-factory
                                                                  selection-autocomplete
                                                                  tags-field]]
            [onekeepass.frontend.constants :as const :refer [ADDITIONAL_ONE_TIME_PASSWORDS
                                                             ONE_TIME_PASSWORD_TYPE]]
            [onekeepass.frontend.db-icons :as db-icons :refer [entry-icon
                                                               entry-type-icon]]
            [onekeepass.frontend.entry-form.common :as ef-cmn :refer [background-color1
                                                                      content-sx
                                                                      ENTRY_DATETIME_FORMAT
                                                                      popper-box-sx
                                                                      popper-button-sx]]
            [onekeepass.frontend.entry-form.dialogs :as dlg :refer [delete-totp-confirm-dialog
                                                                    set-up-totp-dialog]]
            [onekeepass.frontend.entry-form.fields :refer [datetime-field
                                                           otp-field
                                                           simple-selection-field
                                                           text-area-field
                                                           text-field]]
            [onekeepass.frontend.entry-form.menus :as menus]
            [onekeepass.frontend.events.common :as ce]
            [onekeepass.frontend.events.entry-form-dialogs :as dlg-events]
            [onekeepass.frontend.events.entry-form-ex :as form-events]
            [onekeepass.frontend.events.move-group-entry :as move-events]
            [onekeepass.frontend.group-tree-content :as gt-content]
            [onekeepass.frontend.mui-components :as m :refer [color-primary-main
                                                              mui-alert
                                                              mui-alert-title
                                                              mui-avatar
                                                              mui-box
                                                              mui-button
                                                              mui-button
                                                              mui-checkbox
                                                              mui-dialog
                                                              mui-dialog-actions
                                                              mui-dialog-content
                                                              mui-dialog-title
                                                              mui-divider
                                                              mui-form-control-label
                                                              mui-grid
                                                              mui-icon-add-circle-outline-outlined
                                                              mui-icon-article-outlined
                                                              mui-icon-button
                                                              mui-icon-button
                                                              mui-icon-delete-outline
                                                              mui-icon-edit-outlined
                                                              mui-icon-save-as-outlined
                                                              mui-link
                                                              mui-list-item
                                                              mui-list-item-avatar
                                                              mui-list-item-text
                                                              mui-list-item-text
                                                              mui-menu-item
                                                              mui-popper
                                                              mui-stack
                                                              mui-text-field
                                                              mui-tooltip
                                                              mui-typography]]
            [onekeepass.frontend.utils :as u :refer [contains-val?
                                                     to-file-size-str]]
            [reagent.core :as r]))

;;(set! *warn-on-infer* true)

(defn on-change-factory
  [handler-name field-name-kw]
  (fn [^js/Event e]
    (if-not (= :protected field-name-kw)
      (handler-name field-name-kw (-> e .-target  .-value))
      (handler-name field-name-kw (-> e .-target  .-checked)))))

(defn on-check-factory [handler-name field-name-kw]
  (fn [e]
    (handler-name field-name-kw (-> e .-target  .-checked))))

(defn on-change-factory2
  [handler-name]
  (fn [^js/Event e]
    ;;(println "e is " (-> e .-target .-value))
    (handler-name  (-> e .-target  .-value))))

;;;;;;;;;;;;;;;;;;;;;; Menu ;;;;;;;;;;;;;;;;
;; Re-exported for use in entry-list
(def form-menu menus/form-menu)

;;;;;;;;;;;;;;;  

(defn box-caption [text]
  [mui-typography {:sx {"&.MuiTypography-root" {:color color-primary-main}}
                   :variant  "button"} text])

(defn expiry-content []
  (let [edit @(form-events/form-edit-mode)
        expiry-duration-selection @(form-events/entry-form-field :expiry-duration-selection)
        ;; :expiry-time is a datetime str in tz UTC of format "2022-12-10T17:36:10"
        ;; chrono::NaiveDateTime is serialized in this format   
        expiry-dt @(form-events/entry-form-data-fields :expiry-time)]
    (when edit
      [mui-box {:sx content-sx}
       [mui-stack {:direction "row" :sx {:align-items "flex-end"}}
        [mui-stack {:direction "row" :sx {:width "30%"}}
         [mui-text-field {:classes {:root "entry-cnt-field"}
                          :select true
                          :label "Expiry Duration"
                          :value  expiry-duration-selection
                       ;;;;TODO Need to work
                          :on-change #(form-events/expiry-duration-selection-on-change (-> % .-target  .-value)) ;;ee/expiry-selection-change-factory
                          :variant "standard" :fullWidth true
                          :style {:padding-right "16px"}}
          [mui-menu-item {:value "no-expiry"} "No Expiry"]
          [mui-menu-item {:value "three-months"} "3 Months"]
          [mui-menu-item {:value "six-months"} "6 Months"]
          [mui-menu-item {:value "one-year"} "1 Year"]
          [mui-menu-item {:value "custom-date"} "Custom Date"]]]
        (when-not (= expiry-duration-selection "no-expiry")
          [mui-stack {:direction "row" :sx {:width "40%"}}
           [datetime-field {:key "Expiry Date"
                            :value (u/to-UTC expiry-dt) #_(u/to-local-datetime-str expiry-dt) ;; datetime str UTC to Local 
                            :on-change-handler (form-events/expiry-date-on-change-factory)}]])]]

      #_(when-not (= expiry-duration-selection "no-expiry")
          [mui-box {:sx content-sx}
           [mui-stack {:direction "row" :sx {}}
            [mui-typography (str "Expires: " (u/to-local-datetime-str expiry-dt "dd MMM yyyy HH:mm:ss p"))]]]))))

(defn uuid-times-content []
  (let [edit @(form-events/form-edit-mode)
        expiry-duration-selection @(form-events/entry-form-field :expiry-duration-selection)
        expiry-dt @(form-events/entry-form-data-fields :expiry-time)
        creation-time @(form-events/entry-form-data-fields :creation-time)
        last-modification-time @(form-events/entry-form-data-fields :last-modification-time)]
    (when-not edit
      [mui-box {:sx content-sx}

       [mui-stack {:direction "row" :sx {:justify-content "space-between" :margin-bottom "10px"}}
        [mui-typography "Uuid:"]
        [mui-typography @(form-events/entry-form-data-fields :uuid)]]

       [mui-stack {:direction "row" :sx {:justify-content "space-between" :margin-bottom "10px"}}
        [mui-typography "Created:"]
        [mui-typography (u/to-local-datetime-str creation-time ENTRY_DATETIME_FORMAT)]]

       [mui-stack {:direction "row" :sx {:justify-content "space-between"}}
        [mui-typography "Last Modified:"]
        [mui-typography (u/to-local-datetime-str last-modification-time ENTRY_DATETIME_FORMAT)]]

       (when-not (= expiry-duration-selection "no-expiry")
         [mui-stack {:direction "row" :sx {:justify-content "space-between" :margin-top "10px"}}
          [mui-typography "Expires:"]
          [mui-typography (u/to-local-datetime-str expiry-dt ENTRY_DATETIME_FORMAT)]])])))

(defn add-modify-section-popper [{:keys [dialog-show
                                         popper-anchor-el
                                         mode
                                         section-name
                                         error-fields] :as dialog-data}]

  [mui-popper {:anchorEl popper-anchor-el
               :id "section"
               :open dialog-show
                ;; need to use z-index as background elements from left panel may be visible when a part of popper is shown
                ;; overlapps that panel  
               :sx {:z-index 2 :min-width "400px"}}
   #_[mui-click-away-listener #_{:onClickAway (fn [e] (println "Clicked outside box" (.-currentTarget e)))}]

   [mui-box {:sx popper-box-sx}
    [mui-stack [mui-typography (if (= mode :add) "New section name" "Modify section name")]]
    [mui-stack [mui-dialog-content {:dividers true}
                [m/text-field {:label "Section Name"
                               :value section-name
                               :error (boolean (seq error-fields))
                               :helperText (get error-fields :section-name)
                               :on-key-press (enter-key-pressed-factory
                                              #(form-events/section-name-add-modify dialog-data))
                               :InputProps {}
                               :on-change (on-change-factory form-events/section-name-dialog-update :section-name)
                               :variant "standard" :fullWidth true}]]]
    [mui-stack  {:sx {:justify-content "end"} :direction "row"}
     [mui-button {:variant "text"
                  :sx popper-button-sx
                  :on-click  #(form-events/section-name-dialog-update :dialog-show false)}
      "Cancel"]
     [mui-button {:variant "text"
                  :sx  popper-button-sx
                  :on-click #(form-events/section-name-add-modify dialog-data)}
      "Ok"]]]])

#_(defn add-modify-section-field-popper
    [{:keys [dialog-show
             popper-anchor-el
             field-name
             protected
             required
             _data-type
             mode
             error-fields]
      :as m}]
    #_[mui-click-away-listener #_{:onClickAway #(form-events/section-field-dialog-update :dialog-show false)}]
    (let [ok-fn (fn [_e]
                  (if (= mode :add)
                    (form-events/section-field-add
                     (select-keys m [:field-name :protected :required :section-name :data-type]))
                    (form-events/section-field-modify
                     (select-keys m [:field-name :current-field-name :data-type :protected :required :section-name]))))]
      [mui-popper {:anchorEl popper-anchor-el
                   :id "field"
                   :open dialog-show
                   :sx {:z-index 2 :min-width "400px"}}
       [mui-box {:sx popper-box-sx}
        [mui-stack [mui-typography (if (= mode :add) "Add field" "Modify field")]]
        [mui-stack
         [mui-dialog-content {:dividers true}
          [mui-stack
           [m/text-field {:label "Field Name"
                     ;; If we set ':value key', the dialog refreshes when on change fires for each key press in this input
                     ;; Not sure why. Using different name like 'field-name' works fine
                          :value field-name
                          :error (boolean (seq error-fields))
                          :helperText (get error-fields field-name)
                          :on-key-press (enter-key-pressed-factory ok-fn)

                        ;; Needs some tweaking as the input remains focus till the error is cleared
                          :inputRef (fn [comp-ref]
                                      (when (and (boolean (seq error-fields)) (not (nil? comp-ref)))
                                        (when-let [comp-id (some-> comp-ref .-props .-id)]
                                          (.focus (.getElementById js/document comp-id)))))
                          :InputProps {}
                          :on-change (on-change-factory form-events/section-field-dialog-update :field-name)
                          :variant "standard" :fullWidth true}]

           [mui-stack {:direction "row"}
            [mui-form-control-label
             {:control (r/as-element
                        [mui-checkbox {:checked protected
                                       :on-change (on-change-factory form-events/section-field-dialog-update :protected)}])
              :label "Protected"}]
            [mui-form-control-label
             {:control (r/as-element
                        [mui-checkbox {:checked required
                                       :on-change (on-check-factory form-events/section-field-dialog-update :required)}])
              :label "Required1"}]]]]]
        [mui-stack  {:sx {:justify-content "end"} :direction "row"}   ;;{:sx {:align-items "end"}}
         [mui-button {:variant "text"
                      :sx popper-button-sx
                      :on-click  (fn [_e]
                                   (form-events/section-field-dialog-update :dialog-show false))} "Cancel"]
         [mui-button {:sx popper-button-sx
                      :variant "text"
                      :on-click ok-fn}
          "Ok"]]]]))

(defn add-modify-section-field-dialog
  [{:keys [dialog-show
           section-name
           field-name
           protected
           _data-type
           mode
           add-more
           error-fields]
    :as m}]
  (let [ok-fn (fn [_e]
                (if (= mode :add)
                  (form-events/section-field-add
                   (select-keys m [:field-name :protected :required :section-name :data-type]))
                  (form-events/section-field-modify
                   (select-keys m [:field-name :current-field-name :data-type :protected :required :section-name]))))]
    [mui-dialog {:open dialog-show :on-click #(.stopPropagation ^js/Event %)
                 ;; This will set the Paper width in all child components 
                 :sx {"& .MuiPaper-root" {:width "60%"}} ;; equivalent to :classes {:paper "pwd-dlg-root"}
                 }
     [mui-dialog-title [mui-stack [mui-typography
                                   (if (= mode :add)
                                     (str "Add field in " section-name)
                                     (str "Modify field in " section-name))]]]
     [mui-dialog-content {:dividers true}
      [mui-stack
       [m/text-field {:label "Field Name"
                     ;; If we set ':value key', the dialog refreshes when on change fires for each key press in this input
                     ;; Not sure why. Using different name like 'field-name' works fine
                      :value field-name
                      :error (boolean (seq error-fields))
                      :helperText (get error-fields field-name)
                      :on-key-press (enter-key-pressed-factory ok-fn)

                        ;; Needs some tweaking as the input remains focus till the error is cleared
                      :inputRef (fn [comp-ref]
                                  (when (and (boolean (seq error-fields)) (not (nil? comp-ref)))
                                    (when-let [comp-id (some-> comp-ref .-props .-id)]
                                      (.focus (.getElementById js/document comp-id)))))
                      :InputProps {}
                      :on-change (on-change-factory form-events/section-field-dialog-update :field-name)
                      :variant "standard" :fullWidth true}]
       [mui-stack {:direction "row"}
        [mui-form-control-label
         {:control (r/as-element
                    [mui-checkbox {:checked protected
                                   :on-change (on-change-factory form-events/section-field-dialog-update :protected)}])
          :label "Protected"}]
        #_[mui-form-control-label
           {:control (r/as-element
                      [mui-checkbox {:checked required
                                     :on-change (on-check-factory form-events/section-field-dialog-update :required)}])
            :label "Required"}]]

       (when add-more [mui-stack
                       [mui-alert {:severity "success" :sx {"&.MuiAlert-root" {:width "100%"}}} ;; need to override the paper width 60%
                        [mui-alert-title "Success"] "Custom field added. You can add more field or cancel to close"]])]]
     [mui-dialog-actions
      [mui-stack  {:sx {:justify-content "end"} :direction "row" :spacing 1}   ;;{:sx {:align-items "end"}}
       [mui-button {:on-click  (fn [_e]
                                 (form-events/section-field-dialog-update :dialog-show false))} "Cancel"]
       [mui-button {:on-click ok-fn}
        "Ok"]]]]))

(defn custom-field-delete-confirm [dialog-data]
  [(alert-dialog-factory "Delete Field"
                         "Are you sure you want to delete this field permanently?"
                         [{:label "Yes" :on-click #(form-events/field-delete-confirm true)}
                          {:label "No" :on-click  #(form-events/field-delete-confirm false)}])
   dialog-data])

(defn custom-section-delete-confirm [dialog-data]
  [(alert-dialog-factory "Delete Field"
                         "Are you sure you want to delete this section and all its fields permanently?"
                         [{:label "Yes" :on-click #(form-events/section-delete-confirm true)}
                          {:label "No" :on-click  #(form-events/section-delete-confirm false)}])
   dialog-data])

(defn section-field-add-icon-button [section-name]
  (if (not= section-name ADDITIONAL_ONE_TIME_PASSWORDS)
    [mui-tooltip {:title "Add a field" :enterDelay 2500}
     [mui-icon-button {:edge "end" :color "primary"
                       :on-click #(form-events/open-section-field-dialog section-name nil)}
      [mui-icon-add-circle-outline-outlined]]]

    [mui-tooltip {:title "Set up TOPT" :enterDelay 2500}
     [mui-icon-button {:edge "end" :color "primary"
                       :on-click #(dlg-events/otp-settings-dialog-show section-name false)}
      [mui-icon-add-circle-outline-outlined]]]))

(defn section-header [section-name]
  (let [edit @(form-events/form-edit-mode)
        standard-sections @(form-events/entry-form-data-fields :standard-section-names)
        comp-ref (atom nil)]
    [mui-stack {:direction "row"
                :ref (fn [e]
                         ;;(println "ref is called " e)
                       (reset! comp-ref e))}
     [mui-stack {:direction "row" :sx {:width "85%"}}
      [box-caption section-name]]
     (when edit
       [mui-stack {:direction "row" :sx {:width "15%" :justify-content "center"}}
        ;; Allow section name change only if the section name is not the standard one
        (when-not (contains-val? standard-sections section-name)
          [:<>
           [mui-tooltip  {:title "Modify section name" :enterDelay 2500}
            [mui-icon-button {:edge "end"
                              :on-click  #(form-events/open-section-name-modify-dialog section-name @comp-ref)}
             [mui-icon-edit-outlined]]]

           [mui-tooltip  {:title "Delete complete section" :enterDelay 2500}
            [mui-icon-button {:edge "end"
                              :on-click  #(form-events/section-delete section-name)}
             [mui-icon-delete-outline]]]])

        [section-field-add-icon-button section-name]
        #_[menus/add-additional-field-menu section-name]
        #_[mui-tooltip {:title "Add a field" :enterDelay 2500}
           [mui-icon-button {:edge "end" :color "primary"
                             :on-click #(form-events/open-section-field-dialog section-name @comp-ref)}
            [mui-icon-add-circle-outline-outlined]]]])]))

(defn section-content [edit section-name section-data group-uuid]
  (let [errors @(form-events/entry-form-field :error-fields)]
    ;; Show a section in edit mode irrespective of its contents; In non edit mode a section is shown only 
    ;; if it has some fields with non blank value. 
    (when (or edit (boolean (seq (filter (fn [kv] (not (str/blank? (:value kv)))) section-data))))  ;;(seq section-data)
      (let [refs (atom {})]
        [mui-box {:sx content-sx}
         [section-header section-name]
         (doall
          (for [{:keys [key value
                        protected
                        required
                        data-type
                        select-field-options
                        standard-field
                        password-score] :as kv} section-data]
          ;; All fields of this section is shown in edit mode. In case of non edit mode, only the  
          ;; fields with values are shown
            (when (or edit (not (str/blank? value))) #_(or edit (or required (not (str/blank? value))))
                  ^{:key key}
                  [mui-stack {:direction "row"
                              :ref (fn [e]
                             ;; We keep a ref to the underlying HtmlElememnt - #object[HTMLDivElement [object HTMLDivElement]] 
                             ;; The ref is kept for each filed's enclosing container 'Stack' component so that we can position the Popper 
                             ;; Sometimes the value of e is nil as react redraws the node
                                     (swap! refs assoc key e))}
                   [mui-stack {:direction "row" :sx {:width (if edit "92%" "100%")}}
                    (cond
                      (not (nil? select-field-options))
                      [simple-selection-field (assoc kv
                                                     :edit edit
                                                     :error-text (get errors key)
                                                     :on-change-handler #(form-events/update-section-value-on-change
                                                                          section-name key (-> % .-target  .-value)))]

                      (= data-type ONE_TIME_PASSWORD_TYPE)
                      [otp-field (assoc kv :edit edit
                                        :section-name section-name
                                        :group-uuid group-uuid)]

                      :else
                      [text-field (assoc kv
                                         :edit edit
                                         :error-text (get errors key)
                                         :password-score password-score
                                         :visible @(form-events/visible? key)
                                         :on-change-handler #(form-events/update-section-value-on-change
                                                              section-name key (-> % .-target  .-value)))])]

                   (when (and edit (not standard-field) (not= data-type ONE_TIME_PASSWORD_TYPE))
                     [mui-stack {:direction "row" :sx {:width "8%" :align-items "flex-end"}}
                      [mui-tooltip  {:title "Modify Field" :enterDelay 2500}
                       [mui-icon-button {:edge "end"
                                         :on-click  #(form-events/open-section-field-modify-dialog
                                                      {:key key
                                                       :protected protected
                                                       :required required
                                                       :popper-anchor-el (get @refs key)
                                                       :section-name section-name})}
                        [mui-icon-edit-outlined]]]
                      [mui-tooltip  {:title "Delete Field" :enterDelay 2500}
                       [mui-icon-button {:edge "end"
                                         :on-click #(form-events/field-delete section-name key)}
                        [mui-icon-delete-outline]]]])])))
         #_[custom-field-delete-confirm @(form-events/field-delete-dialog-data)]
         #_[custom-section-delete-confirm @(form-events/section-delete-dialog-data)]]))))

(defn all-sections-content []
  (let [{:keys [edit showing]
         {:keys [section-names section-fields uuid group-uuid]} :data}
        @(form-events/entry-form-all)
        deleted? @(form-events/is-entry-parent-group-deleted group-uuid)]
    (m/react-use-effect
     (fn []
       #_(println "init - uuid showing edit deleted? : \n" uuid showing edit deleted?)
       (when (and (= showing :selected) (not edit) (not deleted?))
        ;; (println "Will fire entry-form-otp-start-polling")
         (form-events/entry-form-otp-start-polling))

       (fn []
         ;;(println "cleanup - uuid showing edit deleted? : \n" uuid showing edit deleted?))
         (form-events/entry-form-otp-stop-polling uuid)))

         ;; Need to pass the list of all reactive values (dependencies) referenced inside of the setup code or empty list
     (clj->js [uuid showing edit deleted?]))

    [mui-stack
     (doall
      (for [section-name section-names]
        ^{:key section-name} [section-content edit section-name (get section-fields section-name) group-uuid]))]))

(def icons-dialog-flag (r/atom false))

(defn close-icons-dialog []
  (reset! icons-dialog-flag false))

(defn show-icons-dialog []
  (reset! icons-dialog-flag true))

(defn icons-dialog
  ([dialog-open? call-on-icon-selection]
   (fn [dialog-open? call-on-icon-selection]
     [:div [mui-dialog {:open (if (nil? dialog-open?) false dialog-open?)
                        :on-click #(.stopPropagation ^js/Event %) ;;prevents on click for any parent components to avoid closing dialog by external clicking
                        :classes {:paper "group-form-flg-root"}}
            [mui-dialog-title "Icons"]
            [mui-dialog-content {:dividers true}
             [mui-grid {:container true :xs true :spacing 0}
              (for [[idx svg-icon] db-icons/all-icons]
                ^{:key idx} [:div {:style {:margin "4px"} ;;:border "1px solid blue"
                                   :on-click #(do
                                                (call-on-icon-selection idx)
                                                #_(form-events/entry-form-data-update-field-value :icon-id idx)
                                                (close-icons-dialog))} [mui-tooltip {:title "Icon"} svg-icon]])]]
            [mui-dialog-actions
             [mui-button {:variant "contained" :color "secondary"
                          :on-click close-icons-dialog} "Close"]]]]))
  ([dialog-open?]
   [icons-dialog dialog-open? (fn [idx] (form-events/entry-form-data-update-field-value :icon-id idx))]))

(defn title-with-icon-field  []
  ;;(println "title-with-icon-field called ")
  (let [fields @(form-events/entry-form-data-fields [:title :icon-id])
        edit @(form-events/form-edit-mode)
        errors @(form-events/entry-form-field :error-fields)]
    (when edit
      [mui-box {:sx content-sx}
       [mui-stack {:direction "row" :spacing 1}
        [mui-stack {:direction "row" :sx {:width "88%" :justify-content "center"}}
         [text-field {:key "Title"
                      :value (:title fields)
                      :edit true
                      :required true
                      :helper-text "Title of this entry"
                      :error-text (:title errors)
                      :on-change-handler #(form-events/entry-form-data-update-field-value
                                           :title (-> % .-target  .-value))}]]

        [mui-stack {:direction "row" :sx {:width "12%" :justify-content "center" :align-items "center"}}
         [mui-typography {:sx {:padding-left "5px"} :align "center" :paragraph false :variant "subtitle1"} "Icon"]
         [mui-icon-button {:edge "end" :color "primary" :sx {;;:margin-top "16px"
                                                             ;;:margin-right "-8px"
                                                             }
                           :on-click  show-icons-dialog}
          [entry-icon (:icon-id fields)]]]
        [icons-dialog @icons-dialog-flag]]])))

(defn tags-selection []
  (let [;; Both all-tags and tags are vec
        all-tags @(ce/all-tags)
        tags @(form-events/entry-form-data-fields :tags)
        edit @(form-events/form-edit-mode)]
    ;;(println "tags-selection called tags:" tags " all-tags:" all-tags)
    (when (or edit (boolean (seq tags)))
      [mui-box {:sx content-sx}
       [mui-stack {:direction "row"}]
       [tags-field all-tags tags form-events/on-tags-selection edit]
       (when edit
         [mui-typography {:variant "caption"} "Select a tag or start entering a new tag and add"])])))

(defn notes-content []
  (let [edit @(form-events/form-edit-mode)
        notes @(form-events/entry-form-data-fields :notes)]
    (when (or edit (not (str/blank? notes)))
      [mui-box {:sx content-sx}
       [mui-stack {:direction "row"} [box-caption "Notes"]]
       [mui-stack
        [text-area-field {:key "Notes"
                          :value notes
                          :edit edit
                          :on-change-handler (on-change-factory form-events/entry-form-data-update-field-value :notes)
                          #_#(form-events/entry-form-data-update-field-value :notes (-> % .-target  .-value))}]]])))

(defn attachment-delete-confirm-dialog [dialog-data]
  [(alert-dialog-factory "Delete attachment"
                         "Are you sure you want to delete this attachment?"
                         [{:label "Yes" :on-click #(form-events/attachment-delete-dialog-ok)}
                          {:label "No" :on-click  #(form-events/attachment-delete-dialog-close)}])
   dialog-data])

(defn attachments-content []
  (let [edit @(form-events/form-edit-mode)
        attachments @(form-events/attachments)]
    (when (or edit (boolean (seq attachments)))
      [mui-box {:sx content-sx}
       [mui-stack {:direction "row"}
        [mui-stack {:direction "row" :sx {:margin-bottom "10px" :width "90%"}}
         [box-caption "Attachments"]]
        (when edit
          [mui-stack {:direction "row"
                      :sx {:width "10%"
                           :justify-content "flex-end"}}
           [mui-tooltip {:title "Upload a file"}
            [mui-icon-button {:edge "end" :color "primary"
                              :on-click #(form-events/upload-attachment-start)}
             [mui-icon-add-circle-outline-outlined]]]])]

       (doall

        (for [{:keys [key value data-size data-hash] :as kv} attachments]
          ^{:key key} [mui-stack {:direction "row"}
                       (if-not edit
                         ;; View
                         [mui-stack {:direction "row" :sx {:width  "100%"}}
                          [mui-stack {:direction "row" :sx {:width  "90%"}}
                           [mui-stack {:direction "row" :sx {:width  "80%"}}
                            [mui-link {:sx {:color "primary.dark"}
                                       :underline "hover"
                                       :on-click  #(form-events/view-attachment value data-hash)}
                             [mui-typography {:variant "h6" :sx {:font-size "1em" :align-self "center"}}
                              value]]]
                           [mui-stack {:direction "row" :sx {:width  "20%"}}
                            [mui-typography {:variant "h1" :sx {:font-size ".9em" :align-self "center"}}
                             (to-file-size-str data-size)]]]
                          [mui-stack {:direction "row"
                                      :sx {:width "10%"
                                           :align-items "flex-end"
                                           :justify-content "flex-end"}}
                           [mui-tooltip  {:title "View" :enterDelay 2500}
                            [mui-icon-button {:sx {:margin-right 0}
                                              :edge "end"
                                              :on-click #(form-events/view-attachment value data-hash)}
                             [mui-icon-article-outlined]]]
                           [mui-tooltip  {:title "SaveAs" :enterDelay 2500}
                            [mui-icon-button {:sx {:margin-right 0}
                                              :edge "end"
                                              :on-click #(form-events/save-attachment value data-hash)}
                             [mui-icon-save-as-outlined]]]]]

                         ;; Edit
                         [mui-stack {:direction "row" :sx {:width  "100%"}}
                          [mui-stack {:direction "row" :sx {:width "92%"}}
                           [text-field (assoc kv
                                              :no-end-icons true
                                              :edit true
                                              :on-change-handler (on-change-factory2 #(form-events/attachment-name-changed data-hash %)))]]
                          [mui-stack {:direction "row"
                                      :sx {:width "8%"
                                           :align-items "flex-end"
                                           :justify-content "flex-end"}}
                           [mui-tooltip  {:title "Delete" :enterDelay 2500}
                            [mui-icon-button {:sx {:margin-right 0}
                                              :edge "end"
                                              :on-click #(form-events/attachment-delete-confirm-dialog-open data-hash)}
                             [mui-icon-delete-outline]]]]])]))

       [attachment-delete-confirm-dialog @(form-events/attachment-delete-dialog-data)]])))

(defn add-section-content []
  (let [edit @(form-events/form-edit-mode)]
    (when edit
      (let [;;anchor-el (r/atom nil)
            comp-ref (atom nil)]
        [mui-box {;; This box's ref is used as achoring element for the Popper
                  :ref (fn [e]
                         ;;(println "ref is called " e)
                         (reset! comp-ref e))}
         [mui-stack {:direction "row" :sx {:justify-content "center"}}
          [mui-stack {:direction "row"}
           [mui-tooltip  {:title "Add a new section and fields" :enterDelay 2500}
            [mui-icon-button {:edge "end"
                              :on-click #(form-events/open-section-name-dialog @comp-ref)
                              #_(fn [^js/Event _e]
                                  (reset! anchor-el @comp-ref #_(-> e .-currentTarget)))}
             [mui-icon-add-circle-outline-outlined]]]]
          [mui-stack {:direction "row" :sx {:align-items "center" :margin-left "10px"}}
           [mui-tooltip  {:title "Add a new section and fields" :enterDelay 2500}
            [mui-link {:sx {:color "primary.dark"}
                       :underline "hover"
                       :on-click  #(form-events/open-section-name-dialog @comp-ref)
                       #_(fn [^js/Event _e]
                           (reset! anchor-el @comp-ref))}
             [mui-typography {:variant "h6" :sx {:font-size "1.1em"}}
              "Add Section"]]]]]
         [add-modify-section-popper @(form-events/section-name-dialog-data)]
         #_[add-section-popper anchor-el]]))))

(defn center-content []
  (fn []
    [mui-box
     [title-with-icon-field]
     [:f>  all-sections-content]
     [add-section-content]
     [notes-content]
     [tags-selection]
     [attachments-content]
     [uuid-times-content]
     [expiry-content]]))

(defn delete-permanent-dialog [dialog-data entry-uuid]
  [(alert-dialog-factory "Entry Delete Permanent"
                         "Are you sure you want to delete this entry permanently?"
                         [{:label "Yes" :on-click #(move-events/delete-permanent-group-entry-ok :entry entry-uuid)}
                          {:label "No" :on-click #(move-events/delete-permanent-group-entry-dialog-show :entry false)}])
   dialog-data])

(defn entry-content []
  (fn []
    (let [title @(form-events/entry-form-data-fields :title)
          icon-id @(form-events/entry-form-data-fields :icon-id)
          entry-uuid  @(form-events/entry-form-data-fields :uuid)
          edit @(form-events/form-edit-mode)
          deleted-cat? @(form-events/deleted-category-showing)
          recycle-bin? @(form-events/recycle-group-selected?)
          group-in-recycle-bin? @(form-events/selected-group-in-recycle-bin?)
          pd-dlg-data  @(move-events/delete-permanent-group-entry-dialog-data :entry)]
      [:div {:class "gbox"
             :style {:margin 0
                     :width "100%"}}

       [:div {:class "gheader" :style {:background background-color1}}
        (when-not edit
          [mui-stack {:direction "row"}
           [mui-stack {:direction "row"  :sx {:width "95%" :justify-content "center"}}
            [entry-icon icon-id]
            [mui-typography {;; Need to use this margin value so that text aligns properly with icon
                             :style {:margin-left 2 :margin-top 2}
                             ;; If we use :sx, we need to use "2px" instead of 2
                             ;; Otherwise mui will interpret as 16px
                             ;;:sx {:margin-left "2px" :margin-top "2px"}
                             :align "center" :paragraph false :variant "h6"} title]]
           [mui-stack {:direction "row" :sx {:width "5%"}}
            [:div {:style {:margin-right "8px"}}
             [form-menu entry-uuid]]]])]

       [:div {:class "gcontent" :style {:overflow-y "scroll"
                                        :background background-color1}}
        [center-content]
        #_[custom-field-dialogs]]

       [:div {:class "gfooter" :style {:margin-top 2
                                       :min-height "46px" ;; needed to align this footer with entry list 
                                       :background m/color-grey-200
                                       ;;:background "var(--mui-color-grey-200)"
                                       }}

        [mui-stack {:sx {:align-items "flex-end"}}
         [:div.buttons1

          (cond
            (or deleted-cat? recycle-bin? group-in-recycle-bin?)
            [:<>
             [mui-button {:variant "contained"
                          :color "secondary"
                          :on-click #(move-events/move-group-entry-dialog-show :entry true)} "Put back"]
             [mui-button {:variant "contained"
                          :color "secondary"
                          :on-click #(move-events/delete-permanent-group-entry-dialog-show :entry true)} "Delete Permanently"]
             [mui-button {:variant "contained"
                          :color "secondary"
                          :on-click form-events/close-on-click} "Close"]]

            edit
            [:<>
             [mui-button {:variant "contained"
                          :color "secondary"
                          :on-click form-events/entry-update-cancel-on-click} "Cancel"]
             [mui-button {:variant "contained"
                          :color "secondary"
                          :disabled  (not @(form-events/modified))
                          :on-click form-events/ok-edit-on-click} "Apply"]]

            :else
            [:<>
             [mui-button {:variant "contained"
                          :color "secondary"
                          :on-click form-events/close-on-click} "Close"]
             [mui-button {:variant "contained"
                          :color "secondary"
                          :on-click form-events/edit-mode-menu-clicked} "Edit"]])

          #_(when (or deleted-cat? recycle-bin? group-in-recycle-bin?)
              [:<>
               [mui-button {:variant "contained"
                            :color "secondary"
                            :on-click #(move-events/move-group-entry-dialog-show :entry true)} "Put back"]
               [mui-button {:variant "contained"
                            :color "secondary"
                            :on-click #(move-events/delete-permanent-group-entry-dialog-show :entry true)} "Delete Permanently"]])
          #_(if edit
              [:<>
               [mui-button {:variant "contained"
                            :color "secondary"
                            :on-click form-events/entry-update-cancel-on-click} "Cancel"]
               [mui-button {:variant "contained"
                            :color "secondary"
                            :disabled  (not @(form-events/modified))
                            :on-click form-events/ok-edit-on-click} "Apply"]]
              [:<>
               [mui-button {:variant "contained"
                            :color "secondary"
                            :on-click form-events/close-on-click} "Close"]
               [mui-button {:variant "contained"
                            :color "secondary"
                            :on-click form-events/edit-mode-menu-clicked} "Edit"]])]]]


       [add-modify-section-popper @(form-events/section-name-dialog-data)]
       [add-modify-section-field-dialog @(form-events/section-field-dialog-data)]

       [set-up-totp-dialog @(dlg-events/otp-settings-dialog-data)]
       [delete-totp-confirm-dialog @ef-cmn/delete-totp-confirm-dialog-data]
       [custom-field-delete-confirm @(form-events/field-delete-dialog-data)]
       [custom-section-delete-confirm @(form-events/section-delete-dialog-data)]

       [gt-content/move-dialog
        {:dialog-data @(move-events/move-group-entry-dialog-data :entry)
         :title "Put back"
         :groups-listing @(form-events/groups-listing)
         :selected-entry-uuid entry-uuid
         :on-change (move-events/move-group-entry-group-selected-factory :entry)
         :cancel-on-click-factory (fn [_data]
                                    #(move-events/move-group-entry-dialog-show :entry false))
         :ok-on-click-factory (fn [data]
                                #(move-events/move-group-entry-ok :entry (:selected-entry-uuid data)))}]

       [delete-permanent-dialog pd-dlg-data entry-uuid]])))

(defn entry-type-group-selection
  "Used in entry new form. Prvides from-1 type component for
   entry type and group selection for an entry"
  []
  (let [groups-listing (form-events/groups-listing)
        group-selected (form-events/entry-form-field :group-selection-info)
        entry-type-headers (ce/all-entry-type-headers)
        entry-type-uuid @(form-events/entry-form-data-fields :entry-type-uuid)
        field-error-text (:group-selection @(form-events/entry-form-field :error-fields))]
    ;;(println "entry-type-uuid is " entry-type-uuid)
    [mui-box {:sx content-sx}
     [mui-stack {:spacing 1}
      [mui-stack {:direction "row" :sx {:width "100%"}}
       [mui-stack {:direction "row" :sx {:width "90%"}}
        [mui-text-field  {:id  "select"
                          :select true
                          :required true
                          :label "Entry Type"
                          :value entry-type-uuid ;;field-type
                          :helper-text "An entry's type determines available fields"
                          ;;:InputProps {:classes {:focused "dialog-field-edit-focused"}}
                          :on-change (on-change-factory2 form-events/entry-type-uuid-on-change) #_de/custom-field-type-edit-on-change
                          :variant "standard" :fullWidth true}
         ;; select fields options
         (doall
          (for [{:keys [name uuid]} @entry-type-headers]
            ^{:key uuid} [mui-menu-item {:value uuid} name]))]]
       [mui-stack {:direction "row"
                   :sx {:width "10%"
                        :align-items "center"
                        :justify-content "center"}}
        [mui-tooltip {:title "Add custom entry type" :enterDelay 2500}
         [mui-icon-button {:edge "end"
                           :color "primary"
                           :on-click form-events/new-custom-entry-type}
          [mui-icon-add-circle-outline-outlined]]]]]


      [selection-autocomplete {:label "Group/Category"
                               :options @groups-listing
                               :current-value @group-selected
                               :on-change form-events/on-group-selection
                               :required true
                               :helper-text "An entry's group/category"
                               :error (not (nil? field-error-text))
                               :error-text field-error-text}]]]))

(defn entry-content-new []
  ;;(println "entry-content-new called")
  (let [title @(form-events/entry-form-data-fields :title)
        form-title (if (str/blank? title) "New Entry" (str "New Entry" "-" title))]
    [:div {:class "gbox"
           :style {:margin 0
                   :width "100%"}}

     [:div {:class "gheader" :style {:background background-color1}}
      [mui-stack {:direction "row"  :sx {:width "100%" :justify-content "center"}}
       [mui-typography {:align "center"
                        :paragraph false
                        :variant "h6"}
        form-title]]]
     [:div {:class "gcontent" :style {:overflow-y "scroll"
                                      :background background-color1}}
      [entry-type-group-selection]
      [center-content]
      #_[custom-field-dialogs]]

     [:div {:class "gfooter" :style {:margin-top 2
                                     :min-height "46px" ;; needed to align this footer with entry list 
                                     :background "var(--mui-color-grey-200)"}}

      [mui-stack {:sx {:align-items "flex-end"}}
       [:div.buttons1
        [mui-button {:variant "contained" :color "secondary"
                     :on-click form-events/new-entry-cancel-on-click #_ee/new-entry-cancel-on-click} "Cancel"]
        [mui-button {:variant "contained" :color "secondary"
                     :on-click form-events/ok-new-entry-add} "Ok"]]]]

     [add-modify-section-popper @(form-events/section-name-dialog-data)]
     [add-modify-section-field-dialog @(form-events/section-field-dialog-data)]
     [set-up-totp-dialog @(dlg-events/otp-settings-dialog-data)]]))

(defn entry-content-welcome
  []
  (let [text-to-show @(form-events/entry-form-field :welcome-text-to-show)]
    [:div {:class "gbox"
           :style {:margin 0
                   :width "100%"
                   :background "var(--secondary)"}}
     [:div {:class "gheader"}]
     [:div {:class "gcontent"}
      [mui-stack {:sx {:height "100%"
                       :align-items "center"
                       :justify-content "center"}}
       [mui-typography  {:variant "h6"}
        (if (not (nil? text-to-show))
          text-to-show
          "No entry content is selected. Select or create a new one")]]]
     [:div {:class "gfooter"}]]))

(declare history-entry-content)

(declare custom-entry-type-new-content)

(defn entry-content-core []
  (let [s (form-events/entry-form-field :showing)]
    (condp = @s
      :welcome
      [entry-content-welcome]

      :new
      [entry-content-new]

      :selected
      [entry-content]

      :history-entry-selected
      [history-entry-content]

      :custom-entry-type-new
      [custom-entry-type-new-content]

      [entry-content-welcome])))

;;;;;;;;;;;;;;;;;;;;;;;;; Custom Entry Type ;;;;;;;;;;;;;;;;;;;;;;;;

(defn entry-type-section-content [edit section-name section-data]
  (let [errors @(form-events/entry-form-field :error-fields)]
    [mui-box {:sx content-sx}
     [section-header section-name]
     (doall
      (for [{:keys [key
                    protected required] :as kv} section-data]
        ^{:key key} [mui-stack {:direction "row"}
                     [mui-stack {:direction "row" :sx {:width (if edit "92%" "100%")}}
                      [text-field (assoc kv
                                         :edit edit
                                         :error-text (get errors key)
                                         :visible @(form-events/visible? key)
                                         :placeholder "Some field value"
                                         :on-change-handler #())]]
                     [mui-stack {:direction "row" :sx {:width "8%" :align-items "flex-end"}}
                      [mui-tooltip  {:title "Modify Field" :enterDelay 2500}
                       [mui-icon-button {:edge "end"
                                         :on-click  #(form-events/open-section-field-modify-dialog {:key key
                                                                                                    :protected protected
                                                                                                    :required required
                                                                                                    :section-name section-name})}
                        [mui-icon-edit-outlined]]]
                      [mui-tooltip  {:title "Delete Field" :enterDelay 2500}
                       [mui-icon-button {:edge "end"
                                         :on-click #(form-events/field-delete section-name key)}
                        [mui-icon-delete-outline]]]]]))

     [custom-field-delete-confirm @(form-events/field-delete-dialog-data)]
     [custom-section-delete-confirm @(form-events/section-delete-dialog-data)]
     [add-modify-section-field-dialog @(form-events/section-field-dialog-data)]]))

(defn entry-type-all-sections-content []
  (let [{:keys [error-fields]
         {:keys [section-names section-fields]} :data} @(form-events/entry-form-all)
        error (:general error-fields)]
    [mui-stack
     [mui-stack
      (doall
       (for [section-name section-names]
         ^{:key section-name} [:<>
                               [entry-type-section-content
                                true section-name
                                (get section-fields section-name)]]))]
     (when error
       [mui-stack
        [mui-alert {:severity "error"} error]])]))

;; TODO: 
;; Duplicate name for entry type is possible as we use uuid to uniquely identify entry types
;; Need to warn user about the name duplication and proceed accordingly
(defn type-name-with-icon-field  []
  (let [{:keys [entry-type-name entry-type-icon-name]} @(form-events/entry-form-data-fields
                                                         [:entry-type-name :entry-type-icon-name])
        errors @(form-events/entry-form-field :error-fields)]
    [mui-box {:sx content-sx}
     [mui-stack {:direction "row" :spacing 1}
      [mui-stack {:direction "row" :sx {:width "90%" :justify-content "center"}}
       [text-field {:key "Entry Type"
                    :value entry-type-name
                    :edit true
                    :required true
                    :helper-text "Entry type name"
                    :error-text (:entry-type-name errors)
                    :on-change-handler (on-change-factory
                                        form-events/entry-form-data-update-field-value
                                        :entry-type-name)}]]

      [mui-stack {:direction "row"
                  :sx {:width "10%"
                       :justify-content "center"
                       :align-items "center"}}
       [mui-typography {:align "center"
                        :paragraph false
                        :variant "subtitle1"} "Icon"]
       [mui-icon-button {:edge "end" :color "primary" :sx {}
                         :on-click  show-icons-dialog}
        [entry-type-icon entry-type-name entry-type-icon-name]]]
      [icons-dialog @icons-dialog-flag (fn [idx]
                                         (form-events/entry-form-data-update-field-value
                                          :entry-type-icon-name (str idx)))]]]))

(defn entry-type-center-content []
  [mui-box
   [type-name-with-icon-field]
   [entry-type-all-sections-content]
   [add-section-content]])

(defn custom-entry-type-new-content []
  [:div {:class "gbox"
         :style {:margin 0
                 :width "100%"}}

   [:div {:class "gheader" :style {:background background-color1}}
    [mui-stack {:direction "row"  :sx {:width "100%" :justify-content "center"}}
     [mui-typography {:align "center"
                      :paragraph false
                      :variant "h6"}
      "New Custom Entry Type"]]]
   [:div {:class "gcontent" :style {:overflow-y "scroll"
                                    :background background-color1}}
    [entry-type-center-content]]

   [:div {:class "gfooter" :style {:margin-top 2
                                   :min-height "46px" ;; needed to align this footer with entry list 
                                   :background "var(--mui-color-grey-200)"}}

    [mui-stack {:sx {:align-items "flex-end"}}
     [:div.buttons1
      [mui-button {:variant "contained" :color "secondary"
                   :on-click form-events/cancel-new-custom-entry-type} "Cancel"]
      [mui-button {:variant "contained" :color "secondary"
                   :on-click form-events/create-custom-entry-type} "Create"]]]]

   #_[cc/message-sanckbar]
   #_[cc/message-sanckbar-alert (merge @(ce/message-snackbar-data) {:severity "info"})]])

;;;;;;;;;;;;;;;;;;;;;;;; Entry History ;;;;;;;;;;;;;;;;;;;;;;

(defn restore-confirm-dialog [dialog-show]
  [(alert-dialog-factory
    "Do you want to replace the current entry?"
    "The existing entry will be replaced with this histrory entry"
    [{:label "Yes" :on-click form-events/restore-entry-from-history}
     {:label "No" :on-click form-events/close-restore-confirm-dialog}])
   {:dialog-show dialog-show}])

(defn delete-confirm-dialog [dialog-show entry-uuid index]
  [(alert-dialog-factory
    "Do you want to delete the selected history entry?"
    "This version of history will be deleted permanently"
    [{:label "Yes" :on-click #(form-events/delete-history-entry-by-index entry-uuid index)}
     {:label "No" :on-click form-events/close-delete-confirm-dialog}])
   {:dialog-show dialog-show}])

(defn delete-all-confirm-dialog [dialog-show entry-uuid]
  [(alert-dialog-factory
    "Do you want to delete all history entries?"
    "All history entries for this entry will be deleted permanently"
    [{:label "Yes" :on-click #(form-events/delete-all-history-entries entry-uuid)}
     {:label "No" :on-click form-events/close-delete-all-confirm-dialog}])
   {:dialog-show dialog-show}])

(defn history-entry-content []
  (fn []
    (let [{:keys [title icon-id]} @(form-events/entry-form-data-fields [:title :icon-id])
          edit @(form-events/form-edit-mode)]
      [:div {:class "gbox"
             :style {:margin 0
                     :width "100%"}}

       [:div {:class "gheader" :style {:background background-color1}}
        (when-not edit
          [mui-stack {:direction "row"}
           [mui-stack {:direction "row"  :sx {:width "95%" :justify-content "center"}}
            [entry-icon icon-id]
            [mui-typography {:align "center" :paragraph false :variant "h6"} title]]])]

       [:div {:class "gcontent" :style {:overflow-y "scroll"
                                        :background background-color1}}
        [center-content]
        [restore-confirm-dialog @(form-events/restore-flag)]
        [delete-confirm-dialog
         @(form-events/delete-flag)
         @(form-events/loaded-history-entry-uuid)
         @(form-events/selected-history-index)]]

       [:div {:class "gfooter" :style {:margin-top 2
                                       :min-height "46px" ;; needed to align this footer with entry list 
                                       :background "var(--mui-color-grey-200)"}}

        [mui-stack {:sx {:align-items "flex-end"}}
         [:div.buttons1
          [mui-button {:variant "contained" :color "secondary"
                       :on-click form-events/show-delete-confirm-dialog} "Delete"]
          [mui-button {:variant "contained" :color "secondary"
                       :on-click form-events/show-restore-confirm-dialog} "Restore"]]]]])))

(defn history-entry-row-item
  "Renders a list item. 
  The arg 'props' is a map passed from 'fixed-size-list'
  "
  [props]
  (fn [props]
    (let [items  (form-events/history-summary-list)
          {:keys [uuid
                  title
                  secondary-title ;; Datetime formatted string in Local tz
                  icon-id
                  history-index]} (nth @items (:index props))
          selected-id (form-events/selected-history-index)]
      ;;(println "props " props)
      ;;(println "secondary-title is " secondary-title)
      [mui-list-item {:style (:style props)
                      ;; Need to work on this
                      ;;:sx {:border-bottom ".0001px solid" :border-color "grey"}
                      :button true
                      :value uuid
                      :on-click #(do
                                   (form-events/update-history-index-selection  uuid history-index))
                      :selected (if (= @selected-id history-index) true false)
                      ;;:secondaryAction (when (= @selected-id (:uuid item)) (r/as-element [mui-icon-button {:edge "end"} [mui-icon-more-vert]]))
                      }
       [mui-list-item-avatar
        [mui-avatar [entry-icon icon-id]]
        #_[mui-avatar [mui-icon-vpn-key-outlined]]]
       [mui-list-item-text
        {:primary title
         :secondary  secondary-title}]])))

(defn history-list-content []
  (let [entry-items (list-items-factory
                     (form-events/history-summary-list)
                     history-entry-row-item
                     :item-size 60
                     :list-style {:max-width nil}
                     :div-style {:width "100%"})]
    [mui-stack {:sx {:width "100%"}}
     [mui-stack {:sx {:height "40px"
                      :justify-content "center"
                      :align-items "center"}}
      [mui-typography {:align "center" :paragraph false :variant "subtitle2"} "Previous Versions"]]

     [entry-items]
     [delete-all-confirm-dialog
      @(form-events/delete-all-flag)
      @(form-events/loaded-history-entry-uuid)]

     [mui-stack {:sx {:min-height "46px"
                      :align-items "center"
                      :background m/color-grey-200}}
      [:div {:style {:margin-top 10 :margin-bottom 10 :margin-right 5 :margin-left 5}}
       [mui-button {:variant "outlined"
                    :color "inherit"
                    :on-click form-events/show-delete-all-confirm-dialog} "Delete All"]]]]))

(defn entry-history-content-main []
  (let [entry-uuid  @(form-events/loaded-history-entry-uuid)]
    ;;(println "entry-uuid is " entry-uuid)
    [mui-stack {:direction "row"
                :divider (r/as-element [mui-divider {:orientation "vertical" :flexItem true}])
                :sx {:height "100%" :overflow-y "scroll"}}
     [mui-stack {:direction "row" :sx {:width "45%"}}
      #_"Some details come here"
      [mui-stack {:direction "row"
                  :divider (r/as-element [mui-divider {:orientation "vertical" :flexItem true}])
                  :sx {:width "100%"}}

       [mui-stack {:direction "row"
                   :sx {:width "50%"
                        :justify-content "center"
                        :align-items "center"}}
        [mui-box
         [mui-typography "Found History Entries"]
         [mui-stack
          [mui-link {:sx {:text-align "center"
                          :color "primary.main"}
                     :underline "always"
                     :variant "subtitle1"
                     :onClick #(form-events/history-content-close entry-uuid)}
           "Back to Entry"]]]]
       [mui-stack {:direction "row" :sx {:width "50%" :overflow-y "scroll"}}
        [history-list-content]]]]
     [mui-stack {:direction "row"
                 :sx {:width "55%"}}
      [entry-content-core]]]))


;;;;;;;;;;;;;;;;;;;
#_(defn custom-field-dialog [{:keys [dialog-show
                                     field-name
                                     field-value
                                     protected
                                     data-type
                                     error-fields
                                     mode
                                     add-more] :as m}]
    [mui-dialog {:open dialog-show :on-click #(.stopPropagation ^js/Event %)
               ;; This will set the Paper width in all child components 
                 :sx {"& .MuiPaper-root" {:width "60%"}} ;; equivalent to :classes {:paper "pwd-dlg-root"}
                 }
     [mui-dialog-title (if (= mode :add) "Add Custom Field" "Modify Custom Field")]
     [mui-dialog-content {:dividers true}
      [mui-stack
       [:<>
        [m/text-field {:label "Field Name"
                     ;; If we set ':value key', the dialog refreshes when on change fires for each key press in this input
                     ;; Not sure why. Using different name like 'field-name' works fine
                       :value field-name
                       :error (boolean (seq error-fields))
                       :helperText (get error-fields field-name)
                       :InputProps {}
                       :on-change (on-change-factory form-events/update-custom-field-dialog-data :field-name)
                       :variant "standard" :fullWidth true}]

        (when (= mode :add)
          [m/text-field {:label "Field Value"
                         :value field-value
                         :InputProps {}
                         :on-change (on-change-factory form-events/update-custom-field-dialog-data :field-value)
                         :variant "standard" :fullWidth true}])

        [mui-form-control-label {:control (r/as-element [mui-checkbox {:checked protected
                                                                       :on-change (on-change-factory form-events/update-custom-field-dialog-data :protected)}])
                                 :label "Protected"}]]

       (when add-more [mui-stack
                       [mui-alert {:severity "success" :sx {"&.MuiAlert-root" {:width "100%"}}} ;; need to override the paper width 60%
                        [mui-alert-title "Success"] "Custom field added. You can add more field or cancel to close"]])]]
     [mui-dialog-actions
      [mui-button {:variant "contained" :color "secondary"
                   :on-click form-events/close-custom-field-dialog} "Cancel"]
      (if (= mode :add)
        [mui-button {:variant "contained" :color "secondary"
                     :on-click #(form-events/custom-field-add (select-keys m [:field-name :field-value :protected :data-type]))} "Add"]
        [mui-button {:variant "contained" :color "secondary"
                     :on-click #(form-events/custom-field-modify (select-keys m [:field-name :current-field-name :protected]))} "Modify"])]])

#_(defn custom-field-dialogs []
    [:<>
     [custom-field-dialog @(form-events/custom-field-dialog-data)]
     [custom-field-delete-confirm @(form-events/field-delete-dialog-data)]])

;; deprecate and keep a copy?
#_(defn custom-fields-content []
    (let [edit @(form-events/form-edit-mode)
          section-data @(form-events/entry-form-section-data section-name-custom-fields)]
      (when (or edit (boolean (seq section-data)))
        [mui-box {:sx content-sx}
         [mui-stack {:direction "row"}
          [mui-stack {:direction "row" :sx {:width "90%"}}
           [box-caption "Custom Fields"]]
          (when edit
            [mui-stack {:direction "row" :sx {:width "10%" :justify-content "flex-end"}}
             [mui-tooltip {:title "Add"}
              [mui-icon-button {:edge "end" :color "primary"
                                :on-click
                                form-events/open-custom-field-dialog}
               [mui-icon-add-circle-outline-outlined]]]])]
         (doall
          (for [{:keys [key] :as kv} section-data]
            ^{:key key} [mui-stack {:direction "row"}
                         [mui-stack {:direction "row" :sx {:width (if edit "92%" "100%")}}
                          [text-field (assoc kv
                                             :edit edit
                                             :visible @(form-events/visible? key)
                                             :on-change-handler #(form-events/update-section-value-on-change section-name-custom-fields key (-> % .-target  .-value)))]]

                         (when edit
                           [mui-stack {:direction "row" :sx {:width "8%" :align-items "flex-end"}}
                            [mui-tooltip  {:title "Modify Custom Field"}
                             [mui-icon-button {:edge "end"
                                               :on-click #(form-events/oepn-custom-field-modify-dialog (select-keys kv [:key :protected]))}
                              [mui-icon-edit-outlined]]]
                            [mui-tooltip  {:title "Delete Custom Field"}
                             [mui-icon-button {:edge "end"
                                               :on-click #(form-events/field-delete section-name-custom-fields key)}
                              [mui-icon-delete-outline]]]])]))])))

;; deprecate
#_(defn add-section-popper [anchor-el]
    (let [{:keys [section-name error-fields]} @(form-events/section-add-data)]
    ;;(println "section-name " section-name "anchor-el is " @anchor-el)
      [mui-popper {:anchorEl @anchor-el
                   :open (if @anchor-el true false)
                   :sx {:z-index 2 :min-width "400px"}}
       [mui-box {:sx {:bgcolor "background.paper"
                      :p "15px"
                      :pb "5px"
                      :border-color "#E7EBF0"
                      :border-width "thin"
                      :border-style "solid"}}
        [mui-stack [mui-typography "Add a section"]]
        [mui-stack [mui-dialog-content {:dividers true}
                    [m/text-field {:label "Section Name"
                                   :value section-name
                                   :error (boolean (seq error-fields))
                                   :helperText (get error-fields :section-name)
                                   :InputProps {}
                                   :on-change (on-change-factory2 form-events/section-add-name-update)
                                   :variant "standard" :fullWidth true}]]]
        [mui-stack  {:sx {:align-items "end"}}
         [mui-button {:variant "text"
                      :color "primary" :on-click (fn [_e]
                                                   (form-events/section-add-done)
                                                   (reset! anchor-el nil))} "Close"]]]]))


