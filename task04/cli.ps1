syndicate generate project --name task04
syndicate generate config
syndicate generate lambda --name sqs_handler --runtime java
syndicate generate meta sqs_queue --resource_name async_queue
