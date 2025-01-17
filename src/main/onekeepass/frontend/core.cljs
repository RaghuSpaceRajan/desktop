(ns  onekeepass.frontend.core  ;;ns ^:figwheel-always onekeepass.frontend.core 
  (:require
   [reagent.dom :as rdom]
   ;;Events 
   [onekeepass.frontend.events.tauri-events :as tauri-events]
   [onekeepass.frontend.events.common :as cmn-events]
   
   ;;App UI components   
   [onekeepass.frontend.common-components :as cc]
   [onekeepass.frontend.entry-form-ex :as eform-ex]
   [onekeepass.frontend.entry-list :as el]
   [onekeepass.frontend.entry-category :as ec]
   [onekeepass.frontend.start-page :as sp]
   [onekeepass.frontend.tool-bar :as tool-bar]

   [onekeepass.frontend.mui-components :as m :refer [split-pane
                                                     custom-theme-atom
                                                     mui-icon-fingerprint
                                                     mui-stack
                                                     mui-box
                                                     mui-tabs
                                                     mui-tab
                                                     mui-typography
                                                     mui-button
                                                     mui-icon-button
                                                     mui-css-baseline
                                                     mui-styled-engine-provider
                                                     mui-theme-provider]]
   [onekeepass.frontend.constants :as const]))

;;(set! *warn-on-infer* true)

(defn right-content []
  [split-pane {:split "vertical"
                 ;;:size "200" 
               :minSize "200"
               :maxSize "275"
               :primary "first"
               :style {:position "relative"}} ;;:background "var(--light)"
   ;; Pane1
   [el/entry-list-content]
   ;; Pane2
   [eform-ex/entry-content-core]])

(defn group-entry-content
  "Shows the group and entry content from the current active db "
  []
  [split-pane {:split "vertical"
               ;;:size "200" 
               :minSize "250"
               :maxSize "260"
               :primary "first"
               :style {:position "relative"}
               ;;:pane2Style {:max-width "25%"}
               :pane1Style {:background "rgba(241, 241, 241, 0.33)"}}
   ;; Pane1
   [ec/entry-category-content]
   ;; Pane2
   [right-content]])

(defn locked-content []
  (let [biometric-type @(cmn-events/biometric-type-available)]
    ;;(println "biometric-type is " biometric-type)
    [mui-stack {:sx {:height "100%"
                     :align-items "center"
                     :justify-content "center"}}
     [mui-box
      [mui-stack {:sx {:align-items "center"}}
       [mui-typography {:variant "h4"} "Database locked"]]

      [mui-stack {:sx {:mt 2}}
       [mui-typography {:variant "h6"}
        @(cmn-events/active-db-key)]]

      [mui-stack {:sx {:mt 3 :align-items "center"}}
       (cond
         (or (= biometric-type const/TOUCH_ID) (= biometric-type const/FACE_ID))
         [mui-icon-button {:aria-label "fingerprint"
                           :color "secondary"
                           :on-click #(cmn-events/unlock-current-db biometric-type)}
          [mui-icon-fingerprint {:sx {:font-size 40}}]]

         :else nil)

       [mui-button {:variant "outlined"
                    :color "inherit"
                    :on-click #(cmn-events/unlock-current-db biometric-type)}
        "Quick unlock"]]]]))

(defn group-entry-content-tabs
  "Presents tabs for all opened dbs.The actual content of a selected 
   tab is determined in 'group-entry-content' through the 'db-key' selection
  The arg 'db-list' is a vector of db summary maps
  "
  [db-list]
  [mui-box
   [mui-box
    [mui-tabs {:value @(cmn-events/active-db-key)
               ;; Sets the active db so that group-entry-content can show selected db data
               :on-change (fn [_event val] (cmn-events/set-active-db-key val))}
     (doall
      (for [{:keys [db-key database-name]} db-list]
        ^{:key db-key}  [mui-tab {:label database-name :value db-key}]))]]])

;; A functional component that uses effect 
(defn main-content []
  (fn []
    (let [content-to-show @(cmn-events/content-to-show)
          db-list @(cmn-events/opened-db-list)

          ;; TODO: 
          ;; Need to explore to use idle-timer to lock a database on onIdle timeout
          ;; For now the simple demo one to print works 
          ;; it (m/use-idle-timer (clj->js {:timeout (* 60 1000) ;; 60 sec
          ;;                                :onIdle (fn [] (println "Idle fired..."))
          ;;                                :onActive (fn [e] (println "Event is called " e))}) [])

          ;; We will not using idle-timer as tracking across multiple db opened is not feasible or may need complex solution
          ;; Instead a simple custom session timeout feature is implemented. See in common.cljs for the implementation
          ]
      ;; (println "idletimer is " it)
      ;;(println "getRemainingTime is " (.getRemainingTime it))

      ;; An example use of react useEffect. This is called one time only
      ;; Need to pass [] as second argument
      ;; (m/react-use-effect (fn [] 
      ;;                       (println " called effect in main")
      ;;                       (fn [] (println "clean up from effect.."))
      ;;                       ) [])

      [mui-stack {:sx {:height "100%"}
                  ;; Tracks the user activity so that session timeout can be initiated if no activity is seen beyond a limit 
                  :on-click cmn-events/user-action-detected}
       ;; Tabs are shown only when there are more than 1 databases are open
       (when (> (count db-list) 1)
         [group-entry-content-tabs db-list])

       ;; A Gap between tab header and content
       [:div {:style {:height "2px"
                      :border-bottom "1px solid rgb(241, 241, 241)"
                      :margin-bottom "2px"}}]
       (cond
         (= content-to-show :group-entry)
         [group-entry-content]

         (= content-to-show :entry-history)
         [eform-ex/entry-history-content-main]

         (= content-to-show :locked-content)
         [locked-content]

         :else
         [group-entry-content])])))

(defn header-bar []
  [:div  [:f> tool-bar/top-bar]])

(defn common-snackbars []
  [:<>
   ;; message-sanckbar shows generic message
   [cc/message-sanckbar]
   ;; message-sanckbar-alert mostly for error notification
   [cc/message-sanckbar-alert]])

(defn root-content
  "A functional component"
  []
  (fn []
    #_(m/react-use-effect (fn [] (println " called effect")) [])

    (if @(cmn-events/show-start-page)
      [:<>
       [sp/welcome-content]
       [common-snackbars]]
      [:div {:class "box"}  ;;:style {:height "100vh"}
       [:div {:class "cust_row header"}
        [header-bar]
        [common-snackbars]]

       [:div {:class "cust_row content" :style {:height "80%"}} ;;height "80%" added for WebKit
        [:f> main-content]]

       ;;At this time, no use case for the footer
       #_[:div {:class "cust_row footer"}
          #_[:span "footer (fixed height)"]
          ;;need to make tag p's margin 0px if we use tag p. Include {:style {:margin 0}}
          [:p "footer (fixed height)"]]])))

(defn main-app []
  ;; Not sure to use :<> or not. Both with or without are working
  [:<>
   ;; Styled using Plain CSS and StylesProvider is used to control the CSS injection order
   ;; See 'Style Library Interoperability' in https://material-ui.com/guides/interoperability/
   [mui-styled-engine-provider {:injectFirst true}
    [mui-theme-provider {:theme @custom-theme-atom} ;;Showing custom theme 
     [mui-css-baseline
      [:f> root-content]]]]])

(defn ^:export init
  "Render the created components to the element that has an id 'app' in index.html"
  []
  (cmn-events/sync-initialize)
  (tauri-events/register-tauri-events)
  (cmn-events/init-session-timeout-tick)
  (rdom/render [main-app]
               (.getElementById  ^js/Window js/document "app")))

;;Renders on load
(init)


;;Renders on load
;; this only gets called once
;;(defonce start-up (do (init) true))




