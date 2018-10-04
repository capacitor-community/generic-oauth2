
  Pod::Spec.new do |s|
    s.name = 'TeamconductorCapacitorOauth2'
    s.version = '1.0.0-alpha.53'
    s.summary = 'Capacitor OAuth2 plugin'
    s.license = 'MIT'
    s.homepage = 'https://github.com/moberwasserlechner/capacitor-oauth2'
    s.author = 'Michael Oberwasserlechner'
    s.source = { :git => 'https://github.com/moberwasserlechner/capacitor-oauth2', :tag => s.version.to_s }
    s.source_files = 'ios/Plugin/Plugin/**/*.{swift,h,m,c,cc,mm,cpp}'
    s.ios.deployment_target  = '10.0'
    s.dependency 'Capacitor'
    s.dependency 'OAuthSwift', '~> 1.2.2'
  end
