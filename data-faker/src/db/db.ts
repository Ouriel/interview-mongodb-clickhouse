import { MongoClient } from 'mongodb'

export let db = null

export async function connect() {
  const client = await MongoClient.connect(
    process.env.DB_URL,
    {
      useNewUrlParser: true,
      replicaSet: process.env.REPLICA_SET_NAME,
    }
  )
  db = client.db('cde')
}
