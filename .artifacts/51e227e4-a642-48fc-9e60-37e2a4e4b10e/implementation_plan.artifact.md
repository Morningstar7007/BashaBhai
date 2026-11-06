# BashaBhai Implementation Plan

This document outlines the step-by-step implementation plan for the **BashaBhai** app. The plan is structured to incrementally build the features while strictly adhering to the lab requirements (Topics 8, 9, 10, and 11).

## Goal Description
Build an Android application that connects university students with off-campus housing (rooms, flats, mess seats). Landlords can post listings, students can search, filter, view on maps, save offline favorites, and contact landlords directly. An admin role will oversee approvals.

## Lab Topics Mapping
*   **Topic 8:** XML Layouts, View Binding, Activity Lifecycle, Click Listeners, Explicit Intents (Navigation), Implicit Intents (Call/SMS).
*   **Topic 9:** Android UI, RecyclerView, Custom View Holders, Adapters, Reusable Layouts.
*   **Topic 10:** Services (Background sync for notifications), Broadcast Receivers (Network state monitoring).
*   **Topic 11:** Firebase Database (Data Handling), Room SQLite (Offline Favorites), Network Requests.

## Proposed Steps

### Step 1: Project Setup & Authentication (Topic 8 & 11)
*   **Firebase Setup:** Connect the project to Firebase and add `google-services.json`.
*   **Architecture & ViewBinding:** Enable View Binding in `build.gradle`.
*   **UI Layouts:** Create basic XML layouts for Login, Registration, and Main Dashboard.
*   **Activities:** Create `LoginActivity`, `RegisterActivity`, and `MainActivity`.
*   **Navigation:** Use Explicit Intents to navigate between these screens based on authentication state.
*   **Firebase Auth:** Implement basic Email/Password authentication.

### Step 2: Main Dashboard & Listings UI (Topic 9)
*   **Data Model:** Create a `Listing` data class (Rent, Amenities, Location, Type, etc.).
*   **Custom Item Layout:** Create `item_listing.xml` for individual posts.
*   **RecyclerView:** Implement `ListingAdapter` and `ViewHolder` to display a scalable list of housing options in `MainActivity`.

### Step 3: Database & Cloud Integration (Topic 11)
*   **Firebase Realtime Database / Firestore:** Set up the database structure for Listings and Users (Student/Landlord/Admin).
*   **Add Listing Feature:** Create `AddListingActivity` for Landlords to push new data to Firebase.
*   **Fetch Data:** Read data from Firebase to populate the RecyclerView in `MainActivity`.

### Step 4: Interactivity & Device Capabilities (Topic 8)
*   **Detail View:** Create a `ListingDetailActivity` using Explicit Intents to pass listing data.
*   **Contact Options:** Implement "Call Landlord" and "SMS" buttons using Implicit Intents.
*   **Map Integration:** Launch Google Maps using an Implicit Intent to show the listing's coordinates.

### Step 5: Offline Favorites (Topic 11 - Local DB)
*   **Room Database:** Set up Android Room (SQLite) to store favorite listings locally.
*   **Save/Remove:** Add logic to save to or remove from local favorites in `ListingDetailActivity`.
*   **Favorites UI:** Create `FavoritesActivity` with its own RecyclerView to display locally saved items even when offline.

### Step 6: Background Operations (Topic 10)
*   **Broadcast Receiver:** Implement a `NetworkChangeReceiver` to detect and notify the user when they go offline.
*   **Service:** Implement a background `Service` (or `Worker`) that checks for new listings matching a student's criteria and triggers local notifications.

## Open Questions
*   **Firebase Configuration:** Do you already have a Firebase Project created, or would you like me to guide you on how to set it up and get the `google-services.json` file?
*   **UI Design:** Do you have any specific color themes or design preferences, or should I use standard Material Design components?

## User Review Required
Please review the steps above. Once you approve, we will begin with **Step 1**.