maintenance plan has now no relationship with skill and capability table,
bcz they are not built while iam building asset management class,
after building skill and capability table, map them with these tables


Known Limitation:

Concurrent odometer updates may pass validation
simultaneously because latest reading lookup and
insert are separate operations.

Future improvement:

Use optimistic locking on Asset
or pessimistic locking on latest reading query
to guarantee monotonic odometer history under
concurrent updates.

JWT
↓
userId=15
↓
CurrentUserProvider
↓
15
``

later complete duemaintenanceprojection, repo, and impl