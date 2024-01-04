db = db.getSiblingDB('ria')
db.createCollection('advertisement', { capped: true, size: 100000 })