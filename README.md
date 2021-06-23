# fabric-meta

Fabric Meta is a JSON HTTP API that can be used to query meta data about Fabric's projects. It is updated every 5 mins.

It can be used by tools or launchers that wish to query version information about Fabric.

Hosted at [https://meta.fabricmc.net/](https://meta.fabricmc.net/)

## Endpoints

The versions are in order, the newest versions appear first.

`game_version` and `loader_version` should be url encoded to allow for special characters. For example `1.14 Pre-Release 5` becomes `1.14%20Pre-Release%205`

# V2

### /v2/versions

Full database, includes all the data. **Warning**: large JSON.

### /v2/versions/game

Lists all of the supported game versions.

```json
[
  {
    "version": "1.14",
    "stable": true
  },
  {
    "version": "1.14 Pre-Release 5",
    "stable": false
  }
]
```

### /v2/versions/game/yarn

Lists all of the compatible game versions for yarn.

```json
[
  {
    "version": "1.14.3-pre2",
    "stable": true
  },
  {
    "version": "1.14.3-pre1",
    "stable": false
  }
]
```

### /v2/versions/game/intermediary

Lists all of the compatible game versions for intermediary.

```json
[
  {
    "version": "1.14.3-pre3",
    "stable": true
  },
  {
    "version": "1.14.3-pre2",
    "stable": true
  }
]
```

### /v2/versions/intermediary

Lists all of the intermediary versions, stable is based of the Minecraft version.

```json
[
  {
    "maven": "net.fabricmc:intermediary:1.14.3-pre3",
    "version": "1.14.3-pre3",
    "stable": false
  },
  {
    "maven": "net.fabricmc:intermediary:1.14.3-pre2",
    "version": "1.14.3-pre2",
    "stable": false
  }
]
```

### /v2/versions/intermediary/:game_version

Lists all of the intermediary for the provided game version, there will only ever be 1.

```json
[
  {
    "maven": "net.fabricmc:intermediary:1.14",
    "version": "1.14",
    "stable": true
  }
]
```

### /v2/versions/yarn

Lists all of the yarn versions, stable is based on the Minecraft version.

```json
[
  {
    "gameVersion": "1.14.3-pre2",
    "separator": "+build.",
    "build": 10,
    "maven": "net.fabricmc:yarn:1.14.3-pre2+build.10",
    "version": "1.14.3-pre2+build.10",
    "stable": true
  },
  {
    "gameVersion": "1.14.3-pre2",
    "separator": "+build.",
    "build": 9,
    "maven": "net.fabricmc:yarn:1.14.3-pre2+build.9",
    "version": "1.14.3-pre2+build.9",
    "stable": false
  }
]
```

### /v2/versions/yarn/:game_version

Lists all of the yarn versions for the provided game version.

```json
[
  {
    "gameVersion": "1.14.2",
    "separator": "+build.",
    "build": 7,
    "maven": "net.fabricmc:yarn:1.14.2+build.7",
    "version": "1.14.2+build.7",
    "stable": false
  },
  {
    "gameVersion": "1.14.2",
    "separator": "+build.",
    "build": 6,
    "maven": "net.fabricmc:yarn:1.14.2+build.6",
    "version": "1.14.2+build.6",
    "stable": false
  }
]
```

### /v2/versions/loader

Lists all of the loader versions.

```json
[
  {
    "separator": "+build.",
    "build": 132,
    "maven": "net.fabricmc:fabric-loader:0.4.2+build.132",
    "version": "0.4.2+build.132",
    "stable": true
  },
  {
    "separator": "+build.",
    "build": 131,
    "maven": "net.fabricmc:fabric-loader:0.4.2+build.131",
    "version": "0.4.2+build.131",
    "stable": false
  }
]
```

### /v2/versions/loader/:game_version

This returns a list of all the compatible loader versions for a given version of the game, along with the best version of intermediary to use for that version.

```json
[
  {
    "loader": {
      "separator": "+build.",
      "build": 155,
      "maven": "net.fabricmc:fabric-loader:0.4.8+build.155",
      "version": "0.4.8+build.155",
      "stable": true
    },
    "intermediary": {
      "maven": "net.fabricmc:intermediary:1.14",
      "version": "1.14",
      "stable": true
    }
  },
  {
    "loader": {
      "separator": "+build.",
      "build": 154,
      "maven": "net.fabricmc:fabric-loader:0.4.8+build.154",
      "version": "0.4.8+build.154",
      "stable": false
    },
    "intermediary": {
      "maven": "net.fabricmc:intermediary:1.14",
      "version": "1.14",
      "stable": true
    }
  }
]
```

### /v2/versions/loader/:game_version/:loader_version

This returns the best intermediary for the supplied Minecraft version, as well as the details for the supplied loader version. This should be used if you want to install a specific version of loader along with some intermediary for a specific game version.

Since version 0.1.1 `launcherMeta` is now included, this can be used to get the libraries required by fabric-loader as well as the main class for each side.

```json
{
  "loader": {
    "separator": "+build.",
    "build": 155,
    "maven": "net.fabricmc:fabric-loader:0.4.8+build.155",
    "version": "0.4.8+build.155",
    "stable": true
  },
  "intermediary": {
    "maven": "net.fabricmc:intermediary:1.14",
    "version": "1.14",
    "stable": true
  },
  "launcherMeta": {
    "version": 1,
    "libraries": {
      "client": [
        
      ],
      "common": [
        {
          "name": "net.fabricmc:tiny-mappings-parser:0.1.1.8",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "net.fabricmc:sponge-mixin:0.7.11.36",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "net.fabricmc:tiny-remapper:0.1.0.33",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "net.fabricmc:fabric-loader-sat4j:2.3.5.4",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "com.google.jimfs:jimfs:1.1",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "org.ow2.asm:asm:7.1",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "org.ow2.asm:asm-analysis:7.1",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "org.ow2.asm:asm-commons:7.1",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "org.ow2.asm:asm-tree:7.1",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "org.ow2.asm:asm-util:7.1",
          "url": "https://maven.fabricmc.net/"
        }
      ],
      "server": [
        {
          "_comment": "jimfs in fabric-server-launch requires guava on the system classloader",
          "name": "com.google.guava:guava:21.0",
          "url": "https://maven.fabricmc.net/"
        }
      ]
    },
    "mainClass": {
      "client": "net.fabricmc.loader.launch.knot.KnotClient",
      "server": "net.fabricmc.loader.launch.knot.KnotServer"
    }
  }
}
```

### /v2/versions/loader/:game_version/:loader_version/profile/json

Returns the JSON file that should be used in the standard Minecraft launcher.

### /v2/versions/loader/:game_version/:loader_version/profile/zip

Downloads a zip file with the launcher's profile json, and the dummy jar. To be extracted into .minecraft/versions

### /v2/versions/loader/:game_version/:loader_version/server/json

Returns the JSON file in format of the launcher JSON, but with the server's main class.

# V1

### /v1/versions

Full database, includes all the data. **Warning**: large JSON.

### /v1/versions/game

Lists all of the supported game versions.

```json
[
  {
    "version": "1.14",
    "stable": true
  },
  {
    "version": "1.14 Pre-Release 5",
    "stable": false
  }
]
```

### /v1/versions/game/:game_version

Lists the version information for the game version provided.

`stable` is true for release versions of the game, and false for snapshots

```json
[
  {
    "version": "1.14",
    "stable": true
  }
]
```

### /v1/versions/mappings

Lists all of the mappings versions.

```json
[
  {
    "gameVersion": "1.14",
    "separator": "+build.",
    "build": 1,
    "maven": "net.fabricmc:yarn:1.14+build.1",
    "version": "1.14+build.1",
    "stable": true
  },
  {
    "gameVersion": "1.14 Pre-Release 5",
    "separator": "+build.",
    "build": 8,
    "maven": "net.fabricmc:yarn:1.14 Pre-Release 5+build.8",
    "version": "1.14 Pre-Release 5+build.8",
    "stable": false
  }
]
```

### /v1/versions/mappings/:game_version

Lists all of the mappings for the provided game version.


```json
[
  {
    "gameVersion": "1.14",
    "separator": "+build.",
    "build": 1,
    "maven": "net.fabricmc:yarn:1.14+build.1",
    "version": "1.14+build.1",
    "stable": true
  }
]
```

### /v1/versions/loader

Lists all of the loader versions.

```json
[
  {
    "separator": "+build.",
    "build": 132,
    "maven": "net.fabricmc:fabric-loader:0.4.2+build.132",
    "version": "0.4.2+build.132",
    "stable": true
  },
  {
    "separator": "+build.",
    "build": 131,
    "maven": "net.fabricmc:fabric-loader:0.4.2+build.131",
    "version": "0.4.2+build.131",
    "stable": false
  }
]
```

### /v1/versions/loader/:game_version

This returns a list of all the compatible loader versions for a given version of the game, along with the best version of yarn to use for that version.

```json
[
  {
    "loader": {
      "separator": "+build.",
      "build": 132,
      "maven": "net.fabricmc:fabric-loader:0.4.2+build.132",
      "version": "0.4.2+build.132",
      "stable": true
    },
    "mappings": {
      "gameVersion": "1.14",
      "separator": "+build.",
      "build": 1,
      "maven": "net.fabricmc:yarn:1.14+build.1",
      "version": "1.14+build.1",
      "stable": true
    }
  },
  {
    "loader": {
      "separator": "+build.",
      "build": 131,
      "maven": "net.fabricmc:fabric-loader:0.4.2+build.131",
      "version": "0.4.2+build.131",
      "stable": false
    },
    "mappings": {
      "gameVersion": "1.14",
      "separator": "+build.",
      "build": 1,
      "maven": "net.fabricmc:yarn:1.14+build.1",
      "version": "1.14+build.1",
      "stable": true
    }
  }
]
```

### /v1/versions/loader/:game_version/:loader_version

This returns the best mappings for the supplied Minecraft version, as well as the details for the supplied loader version. This should be used if you want to install a specific version of loader along with some mappings for a specific game version.

Since version 0.1.1 `launcherMeta` is now included, this can be used to get the libraries required by fabric-loader as well as the main class for each side.

```json
{
  "loader": {
    "separator": "+build.",
    "build": 141,
    "maven": "net.fabricmc:fabric-loader:0.4.6+build.141",
    "version": "0.4.6+build.141",
    "stable": true
  },
  "mappings": {
    "gameVersion": "1.14",
    "separator": "+build.",
    "build": 8,
    "maven": "net.fabricmc:yarn:1.14+build.8",
    "version": "1.14+build.8",
    "stable": true
  },
  "launcherMeta": {
    "version": 1,
    "libraries": {
      "client": [
        
      ],
      "common": [
        {
          "name": "net.fabricmc:tiny-mappings-parser:0.1.1.8",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "net.fabricmc:sponge-mixin:0.7.11.30",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "net.fabricmc:tiny-remapper:0.1.0.33",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "net.fabricmc:fabric-loader-sat4j:2.3.5.4",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "com.google.jimfs:jimfs:1.1",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "org.ow2.asm:asm:7.1",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "org.ow2.asm:asm-analysis:7.1",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "org.ow2.asm:asm-commons:7.1",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "org.ow2.asm:asm-tree:7.1",
          "url": "https://maven.fabricmc.net/"
        },
        {
          "name": "org.ow2.asm:asm-util:7.1",
          "url": "https://maven.fabricmc.net/"
        }
      ],
      "server": [
        {
          "_comment": "jimfs in fabric-server-launch requires guava on the system classloader",
          "name": "com.google.guava:guava:21.0",
          "url": "https://maven.fabricmc.net/"
        }
      ]
    },
    "mainClass": {
      "client": "net.fabricmc.loader.launch.knot.KnotClient",
      "server": "net.fabricmc.loader.launch.knot.KnotServer"
    }
  }
}
```
