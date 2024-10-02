syndicate generate project task06
cd task07
syndicate generate config
syndicate generate lambda --name uuid_generator --runtime java
syndicate generate meta s3_bucket --resource_name uuid-storage --location eu-central-1
syndicate generate meta cloudwatch_event_rule `
    --resource_name uuid_trigger `
    --rule_type schedule `
    --expression "rate(1 minute)" `
    --region eu-central-1