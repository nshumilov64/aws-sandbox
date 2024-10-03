syndicate generate project --name task09
syndicate generate config
syndicate generate lambda --name processor --runtime java
syndicate generate meta dynamodb `
    --resource_name Weather `
    --hash_key_name id `
    --hash_key_type S