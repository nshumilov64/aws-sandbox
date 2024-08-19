syndicate generate project --name task05
syndicate generate lambda --name api_handler --runtime java

syndicate generate meta api_gateway `
    --resource_name task5_api `
    --deploy_stage api `
    --minimum_compression_size 0

syndicate generate meta api_gateway_resource `
    --api_name task5_api `
    --path /events `
    --enable_cors false

syndicate generate meta api_gateway_resource_method `
    --api_name task5_api `
    --path /events `
    --method POST `
    --integration_type lambda `
    --lambda_name api_handler



