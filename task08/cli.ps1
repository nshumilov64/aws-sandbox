syndicate generate project --name task08
syndicate generate config
syndicate generate lambda --name api_handler --runtime java
# This is not supported, lol
syndicate generate lambda_layer --name "sdk_layer" --runtime "java" --link_with_lambda "api_handler"