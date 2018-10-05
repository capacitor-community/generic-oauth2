
  Pod::Spec.new do |s|
    s.name = 'ByteowlsCapacitorOauth2'
    s.version = '1.0.0-alpha.1'
    s.summary = 'Capacitor OAuth2 plugin'
    s.license = 'MIT'
    s.homepage = 'https://github.com/moberwasserlechner/capacitor-oauth2'
    s.author = 'Michael Oberwasserlechner'
    s.ios.deployment_target  = '10.0'
    s.dependency 'Capacitor'
    s.dependency 'OAuthSwift', '~> 1.2.2'
    s.source = { :git => 'https://github.com/moberwasserlechner/capacitor-oauth2', :tag => s.version.to_s }
    s.source_files = 'ios/ByteowlsCapacitorOauth2/Sources/**/*.{swift,h,m}'
  end
