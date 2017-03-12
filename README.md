Kryptnostic-Conductor
=======================


Distributed Configuration and Deployment Services.

## Setup
1. Install elasticsearch and its analysis-phonetic plugin
```
brew install elasticsearch
elasticsearch-plugin install analysis-phonetic
```
2. Run elasticsearch with cluster name "loom_development"
```
elasticsearch -E cluster.name=loom_development
```

## Cleanup
Remove all elasticsearch indices and the data they contain
```
curl -XDELETE 'http://localhost:9200/entity_set_data_model/'
curl -XDELETE 'http://localhost:9200/organizations/'
curl -XDELETE 'http://localhost:9200/securable_object_*'
```
