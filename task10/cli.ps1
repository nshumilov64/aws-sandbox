syndicate generate project --name task10
cd task10
syndicate generate config
syndicate generate lambda --name api_handler --runtime java
# Cognito
syndicate generate meta cognito_user_pool --resource_name simple-booking-userpool
# API Gateway
syndicate generate meta api_gateway --resource_name task10_api --deploy_stage api
syndicate generate meta api_gateway_authorizer --api_name task10_api --name authorizer --type COGNITO_USER_POOLS --provider_name simple-booking-userpool
# Gateway resources and methods
syndicate generate meta api_gateway_resource --api_name task10_api --path /signin --enable_cors true
syndicate generate meta api_gateway_resource_method --api_name task10_api --path /signin --method POST --integration_type lambda --lambda_name api_handler
syndicate generate meta api_gateway_resource --api_name task10_api --path /signup --enable_cors true
syndicate generate meta api_gateway_resource_method --api_name task10_api --path /signup --method POST --integration_type lambda --lambda_name api_handler
syndicate generate meta api_gateway_resource --api_name task10_api --path /tables --enable_cors true
syndicate generate meta api_gateway_resource_method --api_name task10_api --path /tables --method GET --integration_type lambda --lambda_name api_handler --authorization_type CUSTOM --authorizer_name authorizer
syndicate generate meta api_gateway_resource_method --api_name task10_api --path /tables --method POST --integration_type lambda --lambda_name api_handler --authorization_type CUSTOM --authorizer_name authorizer
syndicate generate meta api_gateway_resource --api_name task10_api --path "/tables/{tableId}" --enable_cors true
syndicate generate meta api_gateway_resource_method --api_name task10_api --path "/tables/{tableId}" --method GET --integration_type lambda --lambda_name api_handler --authorization_type CUSTOM --authorizer_name authorizer
syndicate generate meta api_gateway_resource --api_name task10_api --path /reservations --enable_cors true
syndicate generate meta api_gateway_resource_method --api_name task10_api --path /reservations --method GET --integration_type lambda --lambda_name api_handler --authorization_type CUSTOM --authorizer_name authorizer
syndicate generate meta api_gateway_resource_method --api_name task10_api --path /reservations --method POST --integration_type lambda --lambda_name api_handler --authorization_type CUSTOM --authorizer_name authorizer
# DynamoDB
syndicate generate meta dynamodb --resource_name Tables --hash_key_name id --hash_key_type S
syndicate generate meta dynamodb --resource_name Reservations --hash_key_name id --hash_key_type S