#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;       // BARU

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform bool useBlinnPhong;
uniform sampler2D textureSampler;
uniform bool useTexture;
uniform sampler2D shadowMap;     // BARU: texture unit 1
uniform bool useShadow;          // BARU
uniform float shadowBias;        // BARU

// ---------------- Shadow Mapping (Indikator 5) ----------------
float calcShadow(vec4 fragPosLS, vec3 norm, vec3 lightDir) {
    vec3 p = fragPosLS.xyz / fragPosLS.w;   // perspective divide
    p = p * 0.5 + 0.5;                      // NDC [-1,1] -> [0,1]
    if (p.z > 1.0) return 1.0;              // di luar far plane cahaya

    // bias dinamis (slope-scaled): makin miring permukaan terhadap cahaya, makin besar
    float bias = max(shadowBias * 10.0 * (1.0 - dot(norm, lightDir)), shadowBias);

    // PCF 3x3: rata-ratakan 9 sampel agar tepi bayangan halus
    vec2 texel = 1.0 / vec2(textureSize(shadowMap, 0));
    float lit = 0.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float dShadow = texture(shadowMap, p.xy + vec2(x, y) * texel).r;
            lit += (p.z - bias > dShadow) ? 0.0 : 1.0;   // d_current > d_shadow + bias -> bayangan
        }
    }
    lit /= 9.0;
    return mix(0.2, 1.0, lit);              // 0.2 = di dalam bayangan, 1.0 = terkena cahaya
}

void main() {
    // 1. Ambient
    vec3 ambient = 0.2 * lightColor;

    // 2. Diffuse (Lambert)
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // 3. Specular (Phong vs Blinn-Phong)
    vec3 viewDir = normalize(viewPos - FragPos);
    float spec = 0.0;
    if (diff > 0.0) {
        if (useBlinnPhong) {
            vec3 halfwayDir = normalize(lightDir + viewDir);
            spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0);
        } else {
            vec3 reflectDir = reflect(-lightDir, norm);
            spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0);
        }
    }
    vec3 specular = 0.8 * spec * lightColor;

    // 4. BARU: bayangan hanya meredam diffuse + specular, ambient tetap
    float shadow = useShadow ? calcShadow(FragPosLightSpace, norm, lightDir) : 1.0;

    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;
    FragColor = vec4((ambient + shadow * (diffuse + specular)) * baseColor, 1.0);
}