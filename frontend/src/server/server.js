import { createServer } from "https";
import next from "next";
import fs from "fs";
import path from "path";
import { createProxyMiddleware } from "http-proxy-middleware";
import express from "express";
import * as http from "node:http";
import * as https from "node:https";
import dotenv from 'dotenv';
import nextConfig from "../../next.config.mjs";

// 1. 항상  .env.local 파일을 로드 (EXPRESS)
dotenv.config({ path: '.env.local' });

// 2. 환경별 `.env` 파일 추가 로드
const envFile = `.env.${process.env.NODE_ENV || 'development'}`;
dotenv.config({ path: envFile, override: true });

const useRemoteAPI = process.env.USE_REMOTE_API === 'true';
const API_URL = useRemoteAPI ? process.env.API_URL_PROD : process.env.API_URL_LOCAL;
const WS_URL = useRemoteAPI ? process.env.WS_URL_PROD : process.env.WS_URL_LOCAL;

console.log("======================================================");
console.log("=== Mode: ", process.env.NODE_ENV);
console.log(`=== Loaded .env.local and ${envFile}`);
console.log("=== API Server: ", API_URL);
console.log("=== WebSocket URL: ", WS_URL);

const dev = process.env.NODE_ENV !== "production";
const app = next({ dev });  // dev 값이 true 인 경우, Next.js 자체가 파일 감시(HMR 포함)를 수행 (Nextjs 영역만)
const handle = app.getRequestHandler();
// 현재 모듈의 디렉토리 경로 구하기
// const __filename = fileURLToPath(import.meta.url);
// const __dirname = path.dirname(__filename);

// Express 서버 생성
const server = express();

// 모든 요청에 대해 요청 정보 로그를 출력하는 미들웨어 설정 (이게 프록시 설정보다 먼저 선언돼야 함)
server.use((req, res, next) => {
    console.log('Request received:', req.method, req.url);  // 요청 메서드와 URL 출력
    next(); // 요청을 다음 미들웨어 또는 라우터로 전달
});

// Express는 기본적으로 마운트 경로(/api)를 제거
// app.use('/api', middleware)로 미들웨어를 등록하면, 미들웨어 내부에서 req.url은 /api가 제거된 나머지 경로로 나타남.
// 예: 클라이언트 요청 /api/auth/login → 미들웨어 내부 req.url은 /auth/login
server.use("/api", (req, res, next) => {
    // req.url = "/api" + req.url;
    console.log("🔥 /path :", req.method, req.url);
    console.log("🔥 /api/path :", req.originalUrl);
    next();
});


// ==========================================
// 🔥 **프록시 설정** (배포환경에서 비활성화)
const isHttps = API_URL.startsWith('https');

const proxyOptions = {
    target: API_URL, // API 서버
    changeOrigin: true,  // 프록시 요청의 Host 헤더를 타겟 서버의 도메인으로 바꿈
    logLevel: 'debug',  // 로그 레벨을 설정하여 프록시 로그 확인 가능,
    secure: false,  // SSL 인증서 검증 비활성화 (로컬 개발용)
    agent: isHttps
        ? new https.Agent({ rejectUnauthorized: false })      // 자체 서명 SSL 허용
        : new http.Agent(),
    selfHandleResponse: false,  // 자동 응답 처리 비활성화 (기본)
};

// Express가 직접 응답 처리 → 브라우저 입장에서는 Express 서버가 보낸 것
const onProxyRes = (proxyRes, req, res) => {
    console.log("=== Express가 대신 쿠키 설정");

    const setCookie = proxyRes.headers['set-cookie'];
    if (setCookie) {
        res.setHeader('Set-Cookie', setCookie);
        delete proxyRes.headers['set-cookie'];
    }
    // proxyRes.pipe(res);  // 수동으로 응답 전달
}

const useReverseProxy = process.env.USE_REVERSE_PROXY === 'true';
if (useReverseProxy) { // Nginx 리버스 프록시 배포 서버 요청 시
    console.log("=== /api prefix 포함 프록시 설정");

    const remoteAPIProxyOptions = {
        pathRewrite: (path, req) => {
            return req.originalUrl; // req.originalUrl는 "/api/auth/login"을 포함함.
        },
    }

    if (dev) {  // 개발환경 전용
        remoteAPIProxyOptions.onProxyRes = onProxyRes;
    }

    Object.assign(proxyOptions, remoteAPIProxyOptions);
} else {    // 로컬 개발용 서버에 요청할 때 (서버 직접 요청)
    console.log("=== Swagger UI 페이지 요청 외 /api prefix 제거 프록시 설정");

    const localAPIProxyOptions = {
        pathRewrite: (path, req) => {
            // swagger 관련 요청에선 그대로 사용
            if (["swagger", "/v3/api-docs"].some(keyword => path.includes(keyword))) {
                return req.originalUrl;
            }
            // 그 외 url에서 api 제거
            return path.replace(/^\/api/, "");
        }
    }

    if (dev) {  // 개발환경 전용
        localAPIProxyOptions.onProxyRes = onProxyRes;
    }

    Object.assign(proxyOptions, localAPIProxyOptions);
}

server.use(
    "/api",
    createProxyMiddleware(proxyOptions)
);


// ==========================================
// Next.js의 기본 라우팅을 처리
server.all("*", (req, res) => {
    return handle(req, res);    // Next.js에서 클라이언트 요청을 처리하는 기본 함수
});

// 서버 실행
const USE_HTTPS = process.env.USE_HTTPS === 'true';
const PORT = process.env.PORT || 3000;

// 💡 basePath 접근
const basePath = nextConfig.basePath || '/';

app.prepare().then(() => {
    if (USE_HTTPS) {
        // HTTPS 서버 실행
        // SSL 인증서 옵션
        const httpsOptions = {
            key: fs.readFileSync(path.resolve(process.env.SSL_KEY_PATH)),       // 개인 키
            cert: fs.readFileSync(path.resolve(process.env.SSL_CERT_PATH)),     // 인증서
        };
        createServer(httpsOptions, server).listen(PORT, (err) => {
            if (err) throw err;
            console.log(`🚀 [HTTPS] Server running at https://localhost:${PORT}${basePath}`);
        });
    } else {
        // HTTP 서버 실행
        server.listen(PORT, () => {
            console.log(`🚀 [HTTP] Server running at http://localhost:${PORT}${basePath}`);
        });
    }

    console.log("======================================================");
});
