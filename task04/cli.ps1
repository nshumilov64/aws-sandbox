syndicate generate project --name task04
syndicate generate config
syndicate generate lambda --name sqs_handler --runtime java
syndicate generate meta sqs_queue --resource_name async_queue
syndicate generate lambda --name sns_handler --runtime java
syndicate generate meta sns_topic --resource_name lambda_topic --region eu-central-1
