#!/bin/bash
#
# Elasticsearch index initialization script for Closet E-Commerce
# Waits for ES to be ready, then creates index templates and indices.
#

set -e

ES_HOST="${ES_HOST:-http://elasticsearch:9200}"
MAX_RETRIES=30
RETRY_INTERVAL=5

echo "=== Closet ES Initializer ==="
echo "Target: $ES_HOST"

# Wait for Elasticsearch to be ready
echo "[1/3] Waiting for Elasticsearch..."
for i in $(seq 1 $MAX_RETRIES); do
  if curl -s "$ES_HOST/_cluster/health" | grep -q '"status"'; then
    echo "  Elasticsearch is ready."
    break
  fi
  if [ "$i" -eq "$MAX_RETRIES" ]; then
    echo "  ERROR: Elasticsearch did not become ready in time."
    exit 1
  fi
  echo "  Attempt $i/$MAX_RETRIES - retrying in ${RETRY_INTERVAL}s..."
  sleep $RETRY_INTERVAL
done

# Create closet-products index
echo "[2/3] Creating closet-products index..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$ES_HOST/closet-products" \
  -H 'Content-Type: application/json' \
  -d '{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "analysis": {
      "tokenizer": {
        "nori_mixed": {
          "type": "nori_tokenizer",
          "decompound_mode": "mixed"
        },
        "autocomplete_tokenizer": {
          "type": "edge_ngram",
          "min_gram": 1,
          "max_gram": 20,
          "token_chars": ["letter", "digit"]
        }
      },
      "analyzer": {
        "nori_analyzer": {
          "type": "custom",
          "tokenizer": "nori_mixed",
          "filter": [
            "nori_readingform",
            "lowercase",
            "nori_part_of_speech"
          ]
        },
        "autocomplete_analyzer": {
          "type": "custom",
          "tokenizer": "autocomplete_tokenizer",
          "filter": ["lowercase"]
        },
        "autocomplete_search_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase"]
        }
      },
      "filter": {
        "nori_part_of_speech": {
          "type": "nori_part_of_speech",
          "stoptags": [
            "E", "IC", "J", "MAG", "MAJ",
            "MM", "SP", "SSC", "SSO", "SC",
            "SE", "XPN", "XSA", "XSN", "XSV",
            "UNA", "NA", "VSV"
          ]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "productId": {
        "type": "long"
      },
      "name": {
        "type": "text",
        "analyzer": "nori_analyzer",
        "fields": {
          "autocomplete": {
            "type": "text",
            "analyzer": "autocomplete_analyzer",
            "search_analyzer": "autocomplete_search_analyzer"
          },
          "keyword": {
            "type": "keyword"
          }
        }
      },
      "brand": {
        "type": "text",
        "analyzer": "nori_analyzer",
        "fields": {
          "keyword": {
            "type": "keyword"
          },
          "autocomplete": {
            "type": "text",
            "analyzer": "autocomplete_analyzer",
            "search_analyzer": "autocomplete_search_analyzer"
          }
        }
      },
      "category": {
        "type": "keyword"
      },
      "subCategory": {
        "type": "keyword"
      },
      "description": {
        "type": "text",
        "analyzer": "nori_analyzer"
      },
      "price": {
        "type": "long"
      },
      "salePrice": {
        "type": "long"
      },
      "discountRate": {
        "type": "integer"
      },
      "colors": {
        "type": "keyword"
      },
      "sizes": {
        "type": "keyword"
      },
      "material": {
        "type": "keyword"
      },
      "fit": {
        "type": "keyword"
      },
      "season": {
        "type": "keyword"
      },
      "gender": {
        "type": "keyword"
      },
      "tags": {
        "type": "keyword"
      },
      "imageUrl": {
        "type": "keyword",
        "index": false
      },
      "reviewCount": {
        "type": "integer"
      },
      "averageRating": {
        "type": "float"
      },
      "salesCount": {
        "type": "long"
      },
      "status": {
        "type": "keyword"
      },
      "createdAt": {
        "type": "date",
        "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
      },
      "updatedAt": {
        "type": "date",
        "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
      }
    }
  }
}')

if [ "$RESPONSE" = "200" ] || [ "$RESPONSE" = "201" ]; then
  echo "  closet-products index created successfully."
elif [ "$RESPONSE" = "400" ]; then
  echo "  closet-products index already exists (skipping)."
else
  echo "  WARNING: Unexpected response code $RESPONSE when creating closet-products index."
fi

# Verify
echo "[3/3] Verifying indices..."
curl -s "$ES_HOST/_cat/indices?v"

echo ""
echo "=== ES initialization complete ==="
