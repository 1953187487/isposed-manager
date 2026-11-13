# Requirements Document — v1.0.8 Enhancement

## Introduction

Isposed Manager v1.0.8 addresses five user-visible problems reported against v1.0.7 and adds three new surfaces.

Reported problems:
1. The TCP activation panel cannot locate ADB-over-TCP devices on the local network.
2. The in-app language selector persists the choice while the interface text stays in the source language.
3. The launch gate shows a blocking agreement dialog on every start and forces the user through consent checkboxes that block progress.
4. The About page is a flat text wall with no application identity, build provenance or navigation to credits.

New surfaces:
5. Per-application authorization management, following AxManager's per-app authorization model and rendered with this project's Miuix design system.
6. A first-run introduction screen describing what the application does.
7. A credits screen listing the people and projects this application builds upon.

## Glossary

- **System**: Isposed Manager, the Android application under specification.
- **Launcher**: The Android launcher application installed on the device.
- **Plain ADB**: An adbd instance reachable over TCP without TLS negotiation, created by `adb tcpip <port>`.
- **TLS ADB**: An adbd instance reachable over TCP with TLS negotiation, advertised through mDNS service `_adb-tls-connect._tcp`.
- **Discovery pass**: One complete sweep of the local subnets and the mDNS service list.
- **Discovery log**: The ordered, timestamped list of step messages produced by a discovery pass.
- **Authorization record**: The persisted grant that associates a package name with a granted permission level.
- **Consent control**: The checkbox on the agreement and open-source notice pages.
- **Introduction screen**: The full-screen product overview shown once before the first use of the application.

## Requirements

### Requirement 1 — LAN device discovery

**User Story:** AS a user AS I want the activation panel to find ADB devices on my local network, SO THAT I can activate without typing IP addresses.

#### Acceptance Criteria

1. WHEN the user starts a discovery pass, THE System SHALL derive each scanned subnet from the network interface prefix length of the active Wi-Fi or Ethernet interface.
2. WHEN the device exposes only a cellular interface, THE System SHALL report that no Wi-Fi or Ethernet network is available and SHALL skip the sweep.
3. WHEN a discovery pass completes, THE System SHALL list every candidate that answered an ADB handshake with its host, port and reported device model.
4. WHEN a candidate answers an authentication challenge without accepting the stored key, THE System SHALL list the candidate and mark it as pending authorization.
5. WHEN the mDNS service list contains TLS ADB services, THE System SHALL merge those services into the device list and mark them as TLS capable.
6. WHILE a discovery pass runs, THE System SHALL display the number of probes completed and the total probe count.
7. IF a discovery pass raises an error, THEN THE System SHALL display the error message and SHALL keep the activation panel usable.
8. WHEN the user selects a discovered device, THE System SHALL fill the host and port fields with that device's endpoint and SHALL place the cursor at the end of the host field.

### Requirement 2 — Activation step log

**User Story:** AS a user AS I want to see each step of the activation attempt, SO THAT I can diagnose a failed connection.

#### Acceptance Criteria

1. WHEN the user starts an activation attempt, THE System SHALL append a log entry for each completed step.
2. WHILE an activation attempt runs, THE System SHALL display the current step name above the log list.
3. IF an activation attempt fails, THEN THE System SHALL append an error entry containing a human-readable cause.
4. WHEN the user copies the log, THE System SHALL place the complete log text on the clipboard.

### Requirement 3 — Per-application authorization

**User Story:** AS a user AS I want to control which applications may use this application's capabilities, SO THAT I can revoke access from any application.

#### Acceptance Criteria

1. WHEN the user opens the authorization screen, THE System SHALL list every installed user application with its current authorization state.
2. WHEN the user grants authorization to an application, THE System SHALL persist an authorization record containing the package name and the granted level.
3. WHEN the user revokes authorization, THE System SHALL remove the authorization record and SHALL reflect the revoked state in the list.
4. WHEN an installed application sends an authorization request to the System, THE System SHALL present a confirmation dialog naming the requesting application.
5. WHEN the user confirms the request, THE System SHALL persist an authorization record and SHALL reply to the requester with the granted result.
6. WHEN the user declines the request, THE System SHALL reply to the requester with the declined result.
7. WHILE an authorization request is pending, THE System SHALL keep the confirmation dialog visible until the user decides.

### Requirement 4 — Interface language

**User Story:** AS a user AS I want the interface to display in my selected language, SO THAT the application is usable in that language.

#### Acceptance Criteria

1. WHEN the user selects a language, THE System SHALL apply that language to all interface text without requiring a restart of the device.
2. WHEN the user selects "follow system", THE System SHALL use the device locale.
3. WHEN the application starts, THE System SHALL render all interface text in the stored language.
4. WHEN the stored language is English, THE System SHALL render every interface label, button and message in English.
5. WHEN the stored language is Simplified Chinese, THE System SHALL render every interface label, button and message in Simplified Chinese.

### Requirement 5 — Onboarding and consent

**User Story:** AS a user AS I want the introduction and notices to inform me without blocking my use of the application, SO THAT I can reach the main screen in one action.

#### Acceptance Criteria

1. WHEN the user reaches the agreement page, THE System SHALL allow the user to advance to the next page regardless of the consent control state.
2. WHEN the user reaches the open-source notice page, THE System SHALL allow the user to finish the flow regardless of the consent control state.
3. WHEN the user completes the onboarding flow, THE System SHALL record the agreement version as accepted.
4. WHEN the application starts with an agreement version already recorded, THE System SHALL skip the onboarding flow.
5. WHEN the application starts with a pending update announcement, THE System SHALL show the announcement before the main screen and SHALL continue to the main screen after the user dismisses it.
6. WHEN the user dismisses the announcement, THE System SHALL keep the main screen reachable in the same session.
7. WHEN the user opens settings, THE System SHALL provide access to the user agreement text and the open-source notice text.

### Requirement 6 — Introduction screen

**User Story:** AS a first-time user AS I want an overview of the application, SO THAT I understand its capabilities before using it.

#### Acceptance Criteria

1. WHEN the user launches the application for the first time, THE System SHALL display the introduction screen.
2. WHILE the introduction screen is visible, THE System SHALL display the application name, the application tagline, a feature summary and a primary action that continues to the onboarding flow.
3. WHEN the user has completed the introduction screen, THE System SHALL record that state and SHALL skip the screen on later launches.
4. WHEN the introduction screen is visible, THE System SHALL keep the back gesture from skipping the screen.

### Requirement 7 — About page

**User Story:** AS a user AS I want to inspect the application's identity and provenance, SO THAT I can verify what I installed.

#### Acceptance Criteria

1. WHEN the user opens the About page, THE System SHALL display the application name, version name, version code and package name.
2. WHEN the user opens the About page, THE System SHALL display the SHA-256 digest of the signing certificate.
3. WHEN the user opens the About page, THE System SHALL provide a navigation entry to the credits screen.
4. WHEN the user opens the About page, THE System SHALL provide a navigation entry to the user agreement text.
5. WHEN the user opens the About page, THE System SHALL list the upstream projects with their licenses.

### Requirement 8 — Credits screen

**User Story:** AS a user AS I want to see the projects and people this application builds upon, SO THAT I can acknowledge their work.

#### Acceptance Criteria

1. WHEN the user opens the credits screen, THE System SHALL display grouped entries covering upstream projects, design system, contributors and testers.
2. WHEN the user opens the credits screen, EACH upstream entry SHALL show the project name, the license and the role the upstream plays in this application.
3. WHEN the user opens the credits screen, THE System SHALL display the author and the project repository address.

## Out of Scope

- A foreground service that exposes a shell binder to other applications.
- Root-based activation.
- Module marketplace distribution.
