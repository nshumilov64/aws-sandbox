syndicate generate meta api_gateway `
    --resource_name task3_api `
    --deploy_stage api `
    --minimum_compression_size 0

syndicate generate meta api_gateway_resource `
    --api_name task3_api `
    --path /hello `
    --enable_cors false

syndicate generate meta api_gateway_resource_method `
    --api_name task3_api `
    --path /hello `
    --method GET `
    --integration_type lambda `
    --lambda_name hello_world