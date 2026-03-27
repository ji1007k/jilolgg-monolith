export async function getToken(data) {
    const utf8Encoded = String.fromCharCode(...new TextEncoder().encode(data));

    const response = await fetch("/token", {
        method: "POST",
        headers: {
            Authorization: `Basic ${btoa(utf8Encoded)}`,
            "Content-Type": "application/json",
        },
    });

    const token = await response.text();
    console.log(token);
    return token;
}

export async function testAdminScope() {
    const token = await getToken("admin:admin");

    const response = await fetch("/admin-page", {
        method: "GET",
        headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
        },
    });

    console.log(await response.text());
}

export async function testUserScope() {
    const token = await getToken("user:password");

    const response = await fetch("/mypage", {
        method: "GET",
        headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
        },
    });

    console.log(await response.text());
}

export async function testUserScopeToAdmin() {
    const token = await getToken("user:password");

    const response = await fetch("/admin-page", {
        method: "GET",
        headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
        },
    });

    console.log(await response.text());
}
