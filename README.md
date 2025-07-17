# Community Leaders Plugin

This plugin allows community leaders to manage their communities effectively by inviting, uninviting, and banning members. It is designed to work with the LuckPerms permission management system.

```bash
# Create group for streamers
/lp creategroup streamer

# Set permissions for the streamer group
/lp group streamer permission set communityleaders.invite true
/lp group streamer permission set communityleaders.uninvite true
/lp group streamer permission set communityleaders.ban true

# Assign the streamer group to a specific user
/lp user <streamer_name> parent add streamer
```