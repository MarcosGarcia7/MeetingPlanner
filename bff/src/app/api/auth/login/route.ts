import { NextResponse } from "next/server";
import { getClientIp } from "@/lib/api-client";
import { createSession } from "@/lib/session";

const API_BASE_URL = 
    process.env.API_INTERNAL_URL ?? "http://localhost:8080";

export async function POST(request: Request): Promise<NextResponse> {
    const headers = new Headers({ "Content-Type": "application/json" });
    const clientIp = getClientIp(request.headers);
    if (clientIp) {
        headers.set("X-Forwarded-For", clientIp);
    }

    const formData = await request.formData();
    const username = formData.get("username");
    const password = formData.get("password");

    try {
        const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: "POST",
        headers,
        body: JSON.stringify({ username, password }),
      });

      if (!response.ok) {
        throw new Error(`HTTP error. Status: ${response.status}`);
      }

      const tokenResponse = await response.json();
      await createSession(tokenResponse.accessToken, tokenResponse.refreshToken);

    } catch (error) {
        console.error("Fetch failed: ", error);
    }
}