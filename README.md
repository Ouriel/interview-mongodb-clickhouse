At the moment npm can't build data-faker due to npm dependency issue (was working last night)
https://github.com/dominictarr/event-stream/issues/116

Here is a simple solution, 
It's pushing all the event into a clickhouse server using scala akka stream

ther is only one table containing everything : default.events
no storage of username and password (GDPR & Security)
There is a second table for auto aggregation of metric 

To run everything
```sh
docker-compose up -d
```
To execute sql query
```sh
docker-compose run clickhouse-client --host clickhouse
```
Answer to question
- Who are the 3 most active users (in term of logins)?
```sql
SELECT userId, 
	count() as cnt 
from events 
where eventType = 'sign-in'
group by userId
order by cnt desc 
limit 3;
```
- What is the percentage of new users?
```sql
select cnt, 
	uniq(userId)
from (
	select 
		userId,
		case when count() <= 1 then 'new' else 'active' end as cnt
	from events 
	where eventType != 'sign-up' 
	group by userId
	) 
group by rollup(cnt);
```

- How many users are active (and what percentage of the users does that represent)?
Same as previous (depends of what is active user)

- What's the average number of signin per active user?
```sql
Select avg(cnt) 
from (
	select userId, 
		count() as cnt 
	from events 
	where eventType = 'sign-in' 
	group by userId
	)
```


Aggregating View-> pre-aggregation pour plus de rapidite
```sql
CREATE MATERIALIZED VIEW default.events_agg
ENGINE = AggregatingMergeTree()
      PARTITION BY toYYYYMM(date)
      ORDER BY (eventType, date)
AS SELECT
    eventType,
    toDate(timestamp) as date,
    countState() AS action,
    uniqState(userId) AS user
FROM default.events
GROUP BY eventType, date;
```
EX de requete
```sql
SELECT
    date,
    countMerge(action) AS action,
    uniqMerge(user) AS Users
FROM default.events_agg
GROUP BY date
```
au lieu de
```sql
select toDate(timestamp) as date, 
	count(), 
	uniq(userId) 
from events 
group by date
```