async function login() {
    const id = document.getElementById("username").value;
    const pwd = document.getElementById("password").value;

    await testAdminScope(await getToken(`${id}:${pwd}`))

}

async function getToken(data) {
    const utf8Encoded = String.fromCharCode(...new TextEncoder().encode(data));

    const response = await fetch("/token", {
        method: "POST",
        headers: {
            "Authorization": `Basic ${btoa(utf8Encoded)}`,
            "Content-Type": "application/json" // 필요에 따라 설정
        }
    });

    const token = await response.text();
    console.log(token);

    return token;
}

async function testAdminScope(token) {
    const response = await fetch("/admin-page", {
        method: "GET",
        headers: {
            "Authorization": `Bearer ${token}`,
            "Content-Type": "application/json"
        }
    });

    console.log(await response.text());
}