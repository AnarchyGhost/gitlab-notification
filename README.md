# Custom Gitlab notifications

A server that accepts gitlab web hooks and converts them into custom notifications

**!!!NOTE: At the moment, this is an MVP with limited functionality, which is planned to be developed in the future**

At the moment works only:
- Push Hook
- Tag Push Hook
- Issue Hook
- Note Hook
- Merge Request Hook
- Wiki Page Hook
- Pipeline Hook
- Job Hook

And sends only to discord.


**Stack**: Kotlin, Ktor

**Build**: jdk 17, gradle 8.4

## Build

```
cd server
./gradlew build
```

Result folder: build/libs

## Installation

1. Download jar from "Releases" sections or build it
2. Place your configuration file and plugins near .jar or configure environment variable ```CONFIG_PATH```
3. Run jar with your jre

#### OR
Using docker
```
docker run -d -p8080:8080 -v ./config:/config -e CONFIG_PATH=exampleConfig/config.json anarchyghost/gitlab-notification:0.0.1 
```

## Usage

1. Write your configuration file
2. Run server
3. Add your server address to gitlab webhooks

## Configuration Description

Example: /exampleConfig/config.json

* plugins - list of paths to jars with custom classes (string list, optional) 
* users - user addition info that can be used in string templates by getUserById, getUserByUsername functions (optional)
  * gitlab (required)
    * id - gitlab user (long, required)
    * username - gitlab username (string, required)
  * discord (optional)
    * id - discord user id (required, string, format <@xxxxxxxxxxxx>)
  * additional (map string to string, optional)
* labels - labels addition info that can be used in string templates by getLabelByName, getLabelsByName functions (optional)
  * gitlab (required)
    * name - label name
  * discord (optional)
    * ids - list of discord ids (required, list string, format <@&xxxxxxxxxxx>)
  * additional (map string to string, optional)
* senders - message senders configuration (required, should contain one of discord/custom)
  * id - sender id, must be unique (required, string)
  * discord - discord sender configuration
    * link - discord web hook link (required)
  * custom - custom sender configuration
    * clazz - full class name (required)
* events - gitlab event listeners configuration
  * type - event type (required, one of PUSH_EVENT,  TAG_EVENT,  ISSUE_EVENT,  COMMENT_EVENT,  MERGE_REQUEST_EVENT,  WIKI_PAGE_EVENT,  PIPELINE_EVENT,  JOB_EVENT,  DEPLOYMENT_EVENT,  FEATURE_FLAG_EVENT,  RELEASE_EVENT,  EMOJI_EVENT,  ACCESS_TOKEN_EVENT,  MEMBER_EVENT,  SUBGROUP_EVENT,  SYSTEM,  OTHER,)
  * projectIds - filter by project id (list string, optional)
  * groupIds - filter by group id (list string, optional)
  * senders - senders that will be used for this event (list, required)
    * id - sender id (string, required)
    * message - message generator info (required, should contain one of custom, discord, text)
      * text - text message configuration
        * content - kotlin string template for message string content (required)
      * discord - discord with embeds message configuration
        * content - kotlin string template for message string content (required)
        * embeds (list, optional)
          * type - kotlin string template that returns one of rich, link, video (required)
          * title - kotlin string template (optional)
          * description - kotlin string template (optional)
          * url - kotlin string template (optional)
      * custom - custom message generator
        * clazz - full class name (required)
  * condition - condition (should contain one of text, labels, or, and, not, custom)
    * text - simple condition (kotlin code, that returns Boolean)
    * labels - condition by labels (should contain one of added, deleted, exists)
      * added - filter by added label (string)
      * deleted - filter by deleted label (string)
      * exists - filter by exists label (string)
    * or - or condition (list of condition)
    * and - and condition (list of condition)
    * not - not condition (condition)
    * custom - custom condition evaluator
      * clazz - full class name (required)

Kotlin functions can use object "data" with fields:
* event - event info (description can be found at https://docs.gitlab.com/ee/user/project/integrations/webhook_events.html).
    * All fields are nullable
    * ids have type long
    * dates have type Instant
* projectId - projectId (string, nullable)
* groupId - groupId (string, nullable)
* labels - labels (set of string, for issue/merge request events)
* addedLabels - added labels (set of string, for issue/merge request events)
* deletedLabels - deleted labels (set of string, for issue/merge request events)
* type - event type, enum EventType
Functions:
* getUserById(id: Long): UsersConfigurationJson
* getUserByUsername(username: String): UsersConfigurationJson
* getLabelByName(name: String): LabelsConfigurationJson
* getLabelsByName(labels: Set<String>): List<LabelsConfigurationJson>

To add custom condition evaluator, message generator, message sender:
1. Implement interface from com.anarchyghost:gitlab-notification-core library
2. Build jar
3. Add jar path to plugins field of config
4. Add className to clazz field of config
Core library: https://github.com/AnarchyGhost/gitlab-notification-core
Custom classes example: https://github.com/AnarchyGhost/gitlab-notification-plugin-example

## TODO

1. More senders
2. More conditions
3. More event types
4. Custom templates language
5. Better class loader
6. Fix bug that custom workers can not work with built-in message types
7. Unit tests

----

[LICENSE](LICENSE)
