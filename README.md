# SP_Lite - Configuration Notes

## Group-based Access Control

Only users that belong to a specific Genesys Cloud group may view the
application data. Non-members are still able to authenticate via OAuth
(Genesys Cloud will validate credentials), but the application will refuse
to run any query, display an error banner, and keep the results table empty.

### Properties (`SP_Lite.properties`)

| Key                      | Default                | Description                                                                 |
|--------------------------|------------------------|-----------------------------------------------------------------------------|
| `enforce_required_group` | `true`                 | If `true`, enforce required group membership after login.                   |
| `required_group_id`      | *(empty)*              | Genesys Cloud group ID that grants access. Leave empty to disable the check. |
| `required_group_name`    | `SearchAndPlay_Users`  | Friendly name shown in the access-denied error message (UI only).           |

### Example

```properties
enforce_required_group = true
required_group_id      = 5bf84c01-7882-45f7-bcd4-a0f851d2c125
required_group_name    = SearchAndPlay_Users
```

### How it works

1. After OAuth login, `Genesys.fetchUserGroups()` calls
   `GET /api/v2/users/me?expand=groups` and collects both group names and
   group IDs into the `GenesysUser` object.
2. `MainController.handleLogin()` compares `required_group_id` with the
   logged-in user's group IDs and stores the result as the session
   attribute `authorized` (`true` / `false`).
3. `MainController.handleSearchCall()` short-circuits when
   `authorized == false`: it never calls the repository, forwards to
   `SearchCall.jsp` with an empty `conversations` list and an `error`
   message that is rendered in the red alert banner at the top of the
   page.

### Disabling the check

Set `enforce_required_group = false` or leave `required_group_id` empty.
In both cases every authenticated user will be treated as authorized.
