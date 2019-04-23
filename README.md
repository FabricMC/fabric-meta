# fabric-meta

Fabric Meta ia a json HTTP api that can be used to query meta data about fabrics projects. It is updated every 5 mins.

I can be used by tools or launchers that wish to query version infomation about fabric

## Endpoints

The versions are in order, the newest versions appear first

`game_version` and `loader_version` should be url encoded to allow for special characters. For example `1.14 Pre-Release 5` becomes `1.14%20Pre-Release%205`

### /v1/versions

full database, includes all the data, warning large json

### /v1/versions/game

Lists all of the supported game versions

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

Lists the version information for the game version provided

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

Lists all of the mappings versions

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

Lists all of the mappings for the provided game version


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

Lists all of the loader versions


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

This returns a list of all the compatible loader versions for a given version of the game, along with the best version of yarn to use for that version

```json
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

This returns the best mappings for the supplied minecraft version, as well as the details for the supplied loader version. This should be used if you want to install a specific version of loader along with some mappings for a specific game version.

```json
{
  "loader": {
    "separator": "+build.",
    "build": 130,
    "maven": "net.fabricmc:fabric-loader:0.4.2+build.130",
    "version": "0.4.2+build.130",
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
}
```