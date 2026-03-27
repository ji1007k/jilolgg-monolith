/** @type {import('next').NextConfig} */
// import path from 'path';

const basePath = '/jikimi';

const nextConfig = {
    // 개발환경에서 렌더링을 2번 유도 -> 잠재적 버그 발견 용이성. nextjs 에선 기본적으로 활성화됨
    reactStrictMode: true,

    // ✅ 서브 경로 설정 추가
    basePath,
    assetPrefix: `${basePath}/`,
    env: {
        NEXT_PUBLIC_BASE_PATH: basePath,
    },
    // ✅ 정적 배포를 위해 추가
    output: 'export',
};

export default nextConfig;
