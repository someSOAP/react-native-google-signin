require 'xcodeproj'
root = File.expand_path('..', __dir__)
path = File.join(root, 'tests/ios/AuthTests.xcodeproj')
project = Xcodeproj::Project.new(path)
target = project.new_target(:unit_test_bundle, 'AuthTests', :ios, '15.1')
['../../ios/GSAuthCoordinator.m', 'GSAuthCoordinatorTests.m'].each do |source|
  target.source_build_phase.add_file_reference(project.main_group.new_file(source))
end
project.build_configurations.each { |config| config.build_settings['CODE_SIGNING_ALLOWED'] = 'NO' }
target.build_configurations.each do |config|
  config.build_settings.merge!({
    'GENERATE_INFOPLIST_FILE' => 'YES', 'PRODUCT_BUNDLE_IDENTIFIER' => 'com.somesoap.AuthTests',
    'HEADER_SEARCH_PATHS' => '$(SRCROOT)/../../ios', 'CLANG_ENABLE_OBJC_ARC' => 'YES',
    'CODE_SIGNING_ALLOWED' => 'NO', 'SUPPORTED_PLATFORMS' => 'iphonesimulator iphoneos'
  })
end
project.save
scheme = Xcodeproj::XCScheme.new
scheme.add_build_target(target)
scheme.add_test_target(target)
scheme.save_as(path, 'AuthTests', true)
