db = db.getSiblingDB('ria')
db.createCollection('advertisementStream', { capped: true, size: 100000 })