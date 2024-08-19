syndicate generate project task06
cd task06
syndicate generate config
syndicate generate lambda --name audit_producer --runtime java
syndicate generate meta dynamodb `
    --resource_name Configuration `
    --hash_key_name key `
    --hash_key_type S
syndicate generate meta dynamodb `
    --resource_name Audit `
    --hash_key_name id `
    --hash_key_type S
