(ns onekeepass.frontend.group-form
  (:require
   [clojure.string :as str]
   [reagent.core :as r]
   [onekeepass.frontend.utils :refer [vec->tags]]
   [onekeepass.frontend.events.common :as cmn-events]
   [onekeepass.frontend.events.group-form :as gf-events]
   [onekeepass.frontend.common-components :refer [tags-field]]
   [onekeepass.frontend.db-icons :as db-icons :refer [group-icon]]
   [onekeepass.frontend.mui-components :as m :refer [mui-dialog
                                                     mui-button
                                                     mui-icon-button
                                                     mui-tooltip
                                                     mui-typography
                                                     mui-divider
                                                     mui-dialog-actions
                                                     mui-dialog-content
                                                     mui-dialog-title
                                                     mui-form-control-label
                                                     mui-checkbox
                                                     mui-grid
                                                     mui-box
                                                     mui-stack]]))
;;(set! *warn-on-infer* true)

(defn form-text-field
  [field-name value on-change editing]
  [m/text-field {:fullWidth true
                 :label field-name :variant "standard"
                 :classes {:root "entry-cnt-field"}
                 :value value
                 :onChange on-change
                 :InputLabelProps {}
                 :InputProps {:id field-name
                              :classes {:root (if editing "entry-cnt-text-field-edit" "entry-cnt-text-field-read")
                                        :focused  (if editing "entry-cnt-text-field-edit-focused" "entry-cnt-text-field-read-focused")}

                              :type "text"}
                         ;;attributes for 'input' tag can be added here
                         ;;It seems adding these 'InputProps' also works
                         ;;We need to use 'readOnly' and not 'readonly'
                 :inputProps  {:readOnly (not editing)}}])

(defn- form-readonly-item [label value]
  [mui-grid {:container true :item true  :wrap "nowrap"}
   [mui-grid {:item true :xs true}
    [mui-typography label]] ;;(:creation-time times) false (:last-modification-time times)

   [mui-grid {:item true :xs true}
    [mui-typography value]]])

(defn- form-readonly-content [times]
  [mui-grid {:container true :item true :spacing 0
             :classes {:root "entry-cnt-container"}
             :style {:width "95%" :margin-top "calc(2*var(--mui-theme-spacing-1))"}}

   [form-readonly-item "Creation Time" (:creation-time times)]
   [mui-grid {:item true :xs true}
    [mui-divider {:variant "fullWidth" :style {:margin "5px 1px 5px 1px"}}]]

   [form-readonly-item "Last Modification Time" (:last-modification-time times)]
   [mui-grid {:item true :xs true}
    [mui-divider {:variant "fullWidth" :style {:margin "5px 1px 5px 1px"}}]]])

(defn- replace-newline [notes]
  (let [s (str/split notes  #"\r\n")]
    (reduce #(conj %1 %2 [:br]) [:div] s)))

(defn- group-info-content
  [{:keys [name tags notes times]}]
  [mui-grid {:container true  :direction "column" :alignItems "center"}
   [mui-grid {:container true :item true :spacing 0
              :classes {:root "entry-cnt-container"}}

    [form-readonly-item "Name" name]
    [mui-grid {:item true :xs true}
     [mui-divider {:variant "fullWidth" :style {:margin "5px 1px 5px 1px"}}]]

    [form-readonly-item "Tags" (vec->tags tags)]

    [mui-grid {:item true :xs true}
     [mui-divider {:variant "fullWidth" :style {:margin "5px 1px 5px 1px"}}]]

    ;;[form-readonly-item "Notes" (replace-newline notes)]
    ;; TODO: Replace this with read only text area 
    (when-not (empty? notes)
      [mui-stack
       [mui-typography "Notes"]
       [mui-stack {:sx {:mt 1}}
        [mui-typography notes]]])
    
    [mui-grid {:item true :xs true}
     [mui-divider {:variant "fullWidth" :style {:margin "5px 1px 5px 1px"}}]]]

   [form-readonly-content times]])

(defn form-textarea-field-1
  [value on-change editing]
  [mui-stack
   [m/text-field {:fullWidth true

                  :id :notes :label "Notes";;name
                  :variant "standard"
                  :value value
                  :onChange on-change
                  :multiline true
                  :rows 4
                  :InputLabelProps {:shrink true}
                  :InputProps {:id :notes}
                  :inputProps  {:readOnly (not editing)
                                :sx {:ml ".5em" :mr ".5em"}
                                :style {:resize "vertical"}}}]])

;;;;;;;;;;;;;;;;; Copied from entry form ;;;;;;;;;;;;;;;;;
;;; Move to common place

(def icons-dialog-flag (r/atom false))

(defn close-icons-dialog []
  (reset! icons-dialog-flag false))

(defn show-icons-dialog []
  (reset! icons-dialog-flag true))

(defn icons-dialog []
  (fn [dialog-open?]
    [:div [mui-dialog {:open (if (nil? dialog-open?) false dialog-open?)
                       :on-click #(.stopPropagation ^js/Event %) ;;prevents on click for any parent components to avoid closing dialog by external clicking
                       :classes {:paper "group-form-flg-root"}}
           [mui-dialog-title "Icons"]
           [mui-dialog-content {:dividers true}
            [mui-grid {:container true :xs true :spacing 0}
             (for [[idx svg-icon] db-icons/all-icons]
               ^{:key idx} [:div {:style {:margin "4px"} ;;:border "1px solid blue"
                                  :on-click #(do
                                               (gf-events/update-form-data :icon-id idx)
                                               (close-icons-dialog))} [mui-tooltip {:title "Icon"} svg-icon]])]]
           [mui-dialog-actions
            [mui-button {:variant "contained" :color "secondary"
                         :on-click close-icons-dialog} "Close"]]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- form-readonly-item-1 [label value]
  [mui-stack
   [mui-stack {:direction "row"}
    [mui-stack {:direction "row" :sx {:width "50%"}} [mui-typography label]]
    [mui-stack {:direction "row" :sx {:width "50%"}} [mui-typography value]]]])

(defn- form-readonly-content-1 [times]
  [mui-box {:sx {:margin-top 2 :border 1}}
   [mui-stack {:sx {:p "5px"}}
    [form-readonly-item-1 "Creation Time" (:creation-time times)]
    [mui-divider {:variant "fullWidth" :style {:margin "5px 1px 5px 1px"}}]
    [form-readonly-item-1 "Last Modification Time" (:last-modification-time times)]]])

(defn- group-content [{:keys [name icon-id tags notes times]}]
  [mui-stack
   [mui-stack {:direction "row" :spacing 1}
    [mui-stack {:direction "row" :sx {:width "90%" :justify-content "center" :align-items "center"}}
     [form-text-field "Name" name (gf-events/form-on-change-factory :name) true]]

    [mui-stack {:direction "row" :sx {:width "10%" :align-items "flex-end"}}
     [mui-typography {:align "center" :paragraph false :variant "subtitle1"} "Icon"]
     [mui-icon-button {:edge "end" :color "primary" :sx {:padding-bottom "5px"}
                       :on-click show-icons-dialog}
      [group-icon icon-id]]]]

   [mui-stack
    [tags-field @(cmn-events/all-tags) tags gf-events/on-tags-selection true]]

   [mui-stack [mui-form-control-label {:control
                                       (r/as-element
                                        [mui-checkbox {:checked @(gf-events/marked-as-category)
                                                       :disabled @(gf-events/showing-groups-as-category)
                                                       :on-change gf-events/marked-as-category-on-check}])
                                       :label "Category"}]]

   [mui-stack
    [form-textarea-field-1 notes (gf-events/form-on-change-factory :notes) true]
    [form-readonly-content-1 times]]

   [icons-dialog @icons-dialog-flag]])

(defn- group-content-dialog [flag form-data mode new-group]
  [:div
   [mui-dialog {:open (if (nil? flag) false flag)
                :on-click #(.stopPropagation ^js/Event %) ;;prevents on click for any parent components to avoid closing dialog by external clicking
                :classes {:paper "group-form-flg-root"}}
    [mui-dialog-title "Group Details"]
    [mui-dialog-content
     (if (= mode :edit)
       [group-content form-data]
       [group-info-content form-data])]

    (if (= mode :edit)
      (let [modified @(gf-events/form-modified)]
        [mui-dialog-actions
         [mui-button {:variant "contained" :color "secondary"
                      :on-click
                      gf-events/cancel-edit-on-click} "Cancel"]
         [mui-button {:variant "contained" :color "secondary"
                      :on-click (if new-group gf-events/ok-new-group-on-click gf-events/ok-edit-on-click)
                      :disabled (not modified)} "Ok"]])
      [mui-dialog-actions
       [mui-button {:variant "contained" :color "secondary"
                    :on-click
                    #(gf-events/close-dialog)} "Cancel"]
       [mui-button {:variant "contained" :color "secondary"
                    :on-click
                    #(gf-events/edit-form)} "Edit"]])]])

(defn group-content-dialog-main []
  (let [{:keys [dialog-open data mode new-group]} @(gf-events/dialog-form-data)]
    [:div [group-content-dialog dialog-open data mode new-group]]))

(comment
  (require '[clojure.string :as str]))