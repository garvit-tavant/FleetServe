maintenance plan has now no relationship with skill and capability table,
bcz they are not built while iam building asset management class,
after building skill and capability table, map them with these tables


JWT
↓
userId=15
↓
CurrentUserProvider
↓
15
``

later complete duemaintenanceprojection, repo, and impl

mapping of asset with depot is done, but no controller is built according to it, so improve it

for technician in findSlotEngine
validFrom <= date
&&
(validTo == null || validTo >= date)