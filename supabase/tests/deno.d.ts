// Just enough of the Deno globals for tsc to check the function outside Deno.
declare const Deno: { env: { get(name: string): string | undefined }; serve(handler: (request: Request) => Response | Promise<Response>): void };
