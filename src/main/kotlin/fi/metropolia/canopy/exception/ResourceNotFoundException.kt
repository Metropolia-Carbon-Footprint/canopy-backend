package fi.metropolia.canopy.exception

class ResourceNotFoundException(resourceName: String, resourceId: Any) :
    RuntimeException("$resourceName with id $resourceId was not found")
